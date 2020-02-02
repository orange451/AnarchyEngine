/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network;

import java.io.IOException;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import engine.Game;
import engine.InternalGameThread;
import engine.io.Save;
import engine.lua.lib.LuaUtil;
import engine.lua.network.internal.GZIPUtil;
import engine.lua.network.internal.NonReplicatable;
import engine.lua.network.internal.PingRequest;
import engine.lua.network.internal.ServerProcessable;
import engine.lua.network.internal.protocol.ClientConnectFinishTCP;
import engine.lua.network.internal.protocol.ClientConnectTCP;
import engine.lua.network.internal.protocol.ClientLoadMapTCP;
import engine.lua.network.internal.protocol.InstanceCreateTCP;
import engine.lua.network.internal.protocol.InstanceDestroyTCP;
import engine.lua.network.internal.protocol.InstanceUpdateUDP;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Player;
import engine.lua.type.object.services.Connections;

public class InternalServer extends Server {
	public static Connection syncConnectionException;
	
	public InternalServer(int port) {
		this.start();
		try {
			this.bind(port, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		InternalRegister.register(this.getKryo());
		
		System.out.println("Server started");
		this.addListener(new Listener() {
			public void received(Connection connection, Object object) {
				
				final int CHUNK_SIZE = 512;

				// Send the game file in chunks of "CHUNK_SIZE" bytes (when client connects)
				if ( object instanceof ClientConnectTCP ) {
					// Grab desired username
					String username = ((ClientConnectTCP)object).username;
					String version = ((ClientConnectTCP)object).version;
					
					// Make sure versions match
					if ( !version.equals(Game.version()) ) {
						connection.close();
						return;
					}
					
					// Check if connection is already established
					Connections connections = ((Connections)Game.getService("Connections"));
					engine.lua.type.object.insts.Connection conInst = connections.getConnectionFromKryo(connection);
					if ( conInst == null ) {
						connection.close();
						return;
					}
					
					conInst.forceSetName(username);
					
					// Load in data
					try {
						JSONParser parser = new JSONParser();
						System.out.println("Attempting to read data: " + ((ClientConnectTCP)object).data);
						JSONObject obj = (JSONObject) parser.parse(((ClientConnectTCP)object).data);
						conInst.forceset(LuaValue.valueOf("Data"), LuaUtil.jsonToTable(obj));
					} catch (ParseException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					// Stream game to client
					String gameJSON = Save.getGameJSON().toJSONString();
					gameJSON = GZIPUtil.compress(gameJSON);
					String[] strings = gameJSON.split("(?<=\\G.{"+CHUNK_SIZE+"})");
					connection.sendTCP(new ClientLoadMapTCP()); // Mark client as "loading map" state.
					new Thread(new Runnable() {

						@Override
						public void run() {
							for (int i = 0; i < strings.length; i++) {
								try {
									connection.sendTCP(new ClientLoadMapTCP(strings[i]));
									
									Thread.sleep(10);
								}catch(Exception e) {
									conInst.disconnect();
								}
							}

							// Tell him it's finished
							connection.sendTCP(new ClientLoadMapTCP(true));
						}
						
					}).start();
				}
				
				// Client finished connecting
				if ( object instanceof ClientConnectFinishTCP ) {
					Connections connections = ((Connections)Game.getService("Connections"));
					engine.lua.type.object.insts.Connection conInst = connections.getConnectionFromKryo(connection);
					if ( conInst == null )
						return;
					
					InternalGameThread.runLater(()->{
						System.out.println("Connecting player...");
						
						// Add player to players folder
						Player player = conInst.connectPlayer();
						
						// Tell client where his player is.
						ClientConnectFinishTCP fn = new ClientConnectFinishTCP();
						fn.SID = player.getSID();
						connection.sendTCP(fn);
					});
				}
				
				if ( object instanceof ServerProcessable ) {
					((ServerProcessable)object).serverProcess(connection);
				}
				
				// Ping request
				if ( object instanceof PingRequest ) {
					((PingRequest)object).process(connection);
				}
			}

			@Override
			public void connected(Connection connection) {
				System.out.println("CONNECTING: " + connection);
				Connections connections = ((Connections)Game.getService("Connections"));
				
				// Create new connection object
				Instance conInst = new engine.lua.type.object.insts.PlayerConnection(connection);
				conInst.forceSetParent(connections);
			}
			
			@Override
			public void disconnected(Connection connection) {
				System.out.println("Disconnecting: " + connection);
				Connections connections = Game.connections();
				if ( connections == null )
					return;
				
				engine.lua.type.object.insts.Connection conInst = connections.getConnectionFromKryo(connection);
				if ( conInst == null )
					return;
				
				conInst.disconnect();
			}
		});
		
		Game.game().descendantAddedEvent().connect((args) -> {
			Instance instance = (Instance) args[0];
			
			// Create instance packet
			InstanceCreateTCP sendObject = new InstanceCreateTCP(instance);
			sendAllTCP(sendObject);
			
			// Sync event. If instance changes, send a update packet.
			syncInstances( instance );
		});
		
		Game.game().descendantRemovedEvent().connect((args) -> {
			Instance instance = (Instance) args[0];
			final long instanceId = instance.getSID();
			
			InternalGameThread.runLater(()->{
				InstanceDestroyTCP destObject = new InstanceDestroyTCP(instanceId);
				sendAllTCP(destObject);
			});
		});
		
		// This should be replaced in the future. Inherent problem with ALREADY existent instances (bandaid)
		List<Instance> objects = Game.game().getDescendants();
		for (int i = 0; i < objects.size(); i++) {
			syncInstances(objects.get(i));
		}
	}

	private static final LuaValue C_NAME = LuaValue.valueOf("Name");
	private static final LuaValue C_PARENT = LuaValue.valueOf("Parent");
	private static final LuaValue C_SID = LuaValue.valueOf("SID");
	
	private void syncInstances(Instance instance) {
		instance.changedEvent().connect((cargs) -> {
			LuaValue key = cargs[0];
			if ( instance instanceof NonReplicatable ) {
				if ( !key.eq_b(C_NAME) && !key.eq_b(C_PARENT) && !key.eq_b(C_SID) ) {
					return;
				}
			}
			
			InstanceUpdateUDP updateObject = new InstanceUpdateUDP(instance, key);
			
			if ( syncConnectionException == null )
				sendAllUDP(updateObject);
			else
				sendAllUDPExcept(updateObject, Game.connections().getConnectionFromKryo(syncConnectionException));
		});
	}
	
	public static void sendAllTCP(Object packet) {
		List<engine.lua.type.object.insts.Connection> cons = ((Connections)Game.getService("Connections")).getConnections();
		for (int i = 0; i < cons.size(); i++) {
			engine.lua.type.object.insts.Connection con = cons.get(i);
			Connection kryo = con.getKryo();
			if ( kryo != null ) {
				kryo.sendTCP(packet);
			}
		}
	}
	
	public static void sendAllUDP(Object packet) {
		sendAllUDPExcept( packet, null );
	}
	
	public static void sendAllUDPExcept(Object packet, engine.lua.type.object.insts.Connection player) {
		List<engine.lua.type.object.insts.Connection> cons = ((Connections)Game.getService("Connections")).getConnections();
		for (int i = 0; i < cons.size(); i++) {
			engine.lua.type.object.insts.Connection con = cons.get(i);
			
			if ( !con.equals(player) ) {
				Connection kryo = con.getKryo();
				if ( kryo != null ) {
					kryo.sendUDP(packet);
				}
			}
		}
	}
}
