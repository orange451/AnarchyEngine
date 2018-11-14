package luaengine.network;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.io.Load;
import ide.ErrorWindow;
import luaengine.LuaEngine;
import luaengine.network.internal.ClientConnectFinishTCP;
import luaengine.network.internal.ClientConnectTCP;
import luaengine.network.internal.ClientLoadMapTCP;
import luaengine.network.internal.ClientProcessable;
import luaengine.network.internal.InstanceCreateTCP;
import luaengine.network.internal.InstanceUpdateUDP;
import luaengine.type.object.Instance;
import luaengine.type.object.insts.PhysicsObject;

public class InternalClient extends Client {
	private String worldJSON;
	private boolean loadingWorld;
	
	public boolean connected;
	
	private static luaengine.type.object.insts.Connection connectionInstance;
	
	public InternalClient( String ip, int port, String username, LuaTable data) {
		this.start();
		try {
			InternalRegister.register(this.getKryo());
			this.connect(5000, ip, port, port);
			connected = true;
			
			this.sendTCP(new ClientConnectTCP(username));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if ( object instanceof ClientLoadMapTCP ) {
					loadingWorld = !loadingWorld;
					if ( !loadingWorld && worldJSON != null ) {
						System.out.println("Loaded world...");
						System.out.println(worldJSON);
						
						try {
							JSONParser parser = new JSONParser();
							final JSONObject obj = (JSONObject) parser.parse(worldJSON);
							
							InternalRenderThread.runLater(()->{
								// Load new game data
								Game.unload();
								Load.parseJSON(obj);
								
								// Create connection object
								connectionInstance = new luaengine.type.object.insts.Connection(connection);
								connectionInstance.forceSetName("ConnectionServer");
								connectionInstance.forceSetParent(Game.getService("Connections"));
								
								// Tell server we're all loaded
								InternalGameThread.runLater(()->{
									connection.sendTCP(new ClientConnectFinishTCP());
								});
								
								Game.setRunning(true);
							});
							
						} catch (Exception e) {
							e.printStackTrace();
							new ErrorWindow("There was a problem reading this file. 001b");
						}
						
						worldJSON = null;
					}
				}
				if ( object instanceof String ) {
					if ( loadingWorld ) {
						if ( worldJSON != null ) {
							worldJSON = worldJSON + object;
						} else {
							worldJSON = (String) object;
						}
					}
				}
				
				if ( object instanceof ClientProcessable ) {
					((ClientProcessable)object).clientProcess();
				}
			}
		});
	}

	public static void sendServerTCP(Object packet) {
		if ( connectionInstance == null )
			return;
		
		if ( connectionInstance.getKryo() == null )
			return;
		
		Connection con = connectionInstance.getKryo();
		con.sendTCP(packet);
	}
	
	public static void sendServerUDP(Object packet) {
		if ( connectionInstance == null )
			return;
		
		if ( connectionInstance.getKryo() == null )
			return;
		
		Connection con = connectionInstance.getKryo();
		con.sendUDP(packet);
	}
}
