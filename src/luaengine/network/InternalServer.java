package luaengine.network;

import java.io.IOException;
import java.util.List;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import engine.Game;
import engine.InternalGameThread;
import engine.io.Save;
import luaengine.network.internal.ClientConnectFinishTCP;
import luaengine.network.internal.ClientConnectTCP;
import luaengine.network.internal.ClientLoadMapTCP;
import luaengine.network.internal.InstanceCreateTCP;
import luaengine.network.internal.InstanceDestroyTCP;
import luaengine.network.internal.InstanceUpdateUDP;
import luaengine.network.internal.PingRequest;
import luaengine.network.internal.ServerProcessable;
import luaengine.type.object.Instance;
import luaengine.type.object.insts.Camera;
import luaengine.type.object.insts.Player;
import luaengine.type.object.services.Connections;

public class InternalServer extends Server {
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
			public void received (Connection connection, Object object) {
				
				final int CHUNK_SIZE = 256;

				// Send the game file in chunks of "CHUNK_SIZE" bytes (when client connects)
				if ( object instanceof ClientConnectTCP ) {
					// Grab desired username
					String username = ((ClientConnectTCP)object).username;
					
					// Check if connection is already established
					Connections connections = ((Connections)Game.getService("Connections"));
					luaengine.type.object.insts.Connection conInst = connections.getConnectionFromKryo(connection);
					if ( conInst == null ) {
						System.out.println("NO CONNECTION");
						//return;
					}
					
					conInst.forceSetName(username);
					
					connection.sendTCP(new ClientLoadMapTCP());
					String gameJSON = Save.getGameJSON().toJSONString();
					String[] strings = gameJSON.split("(?<=\\G.{"+CHUNK_SIZE+"})");
					for (int i = 0; i < strings.length; i++) {
						connection.sendTCP(strings[i]);
					}
					connection.sendTCP(new ClientLoadMapTCP());
				}
				
				// Client finished connecting
				if ( object instanceof ClientConnectFinishTCP ) {
					Connections connections = ((Connections)Game.getService("Connections"));
					luaengine.type.object.insts.Connection conInst = connections.getConnectionFromKryo(connection);
					if ( conInst == null )
						return;
					
					Player player = conInst.connectPlayer();
					
					// Tell client where his player is.
					ClientConnectFinishTCP fn = new ClientConnectFinishTCP();
					fn.SID = player.getSID();
					connection.sendTCP(fn);
				}
				
				if ( object instanceof ServerProcessable ) {
					((ServerProcessable)object).serverProcess();
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
				Instance conInst = new luaengine.type.object.insts.Connection(connection);
				conInst.forceSetParent(connections);
			}
			
			@Override
			public void disconnected(Connection connection) {
				System.out.println("Disconnecting: " + connection);
				Connections connections = ((Connections)Game.getService("Connections"));
				luaengine.type.object.insts.Connection conInst = connections.getConnectionFromKryo(connection);
				if ( conInst == null )
					return;
				
				conInst.disconnect();
			}
		});
		
		Game.game().descendantAddedEvent().connect((args) -> {
			Instance instance = (Instance) args[0];
			
			// DO NOT LEAK PLAYER CONNECTIONS TO CLIENTS
			if ( instance instanceof luaengine.type.object.insts.Connection )
				return;

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
		List<Instance> objects = Game.game().getDescendents();
		for (int i = 0; i < objects.size(); i++) {
			syncInstances(objects.get(i));
		}
	}
	
	private void syncInstances(Instance instance) {
		
		// Keep cameras local
		if ( instance instanceof Camera )
			return;
		
		instance.changedEvent().connect((cargs) -> {
			InternalGameThread.runLater(()->{
				InstanceUpdateUDP updateObject = new InstanceUpdateUDP(instance, cargs[0]);
				sendAllUDP(updateObject);
			});
		});
	}
	
	public static void sendAllTCP(Object packet) {
		List<luaengine.type.object.insts.Connection> cons = ((Connections)Game.getService("Connections")).getConnections();
		for (int i = 0; i < cons.size(); i++) {
			luaengine.type.object.insts.Connection con = cons.get(i);
			Connection kryo = con.getKryo();
			if ( kryo != null ) {
				kryo.sendTCP(packet);
			}
		}
	}
	
	public static void sendAllUDP(Object packet) {
		sendAllUDPExcept( packet, null );
	}
	
	public static void sendAllUDPExcept(Object packet, luaengine.type.object.insts.Connection player) {
		List<luaengine.type.object.insts.Connection> cons = ((Connections)Game.getService("Connections")).getConnections();
		for (int i = 0; i < cons.size(); i++) {
			luaengine.type.object.insts.Connection con = cons.get(i);
			
			if ( !con.equals(player) ) {
				Connection kryo = con.getKryo();
				if ( kryo != null ) {
					kryo.sendUDP(packet);
				}
			}
		}
	}
}
