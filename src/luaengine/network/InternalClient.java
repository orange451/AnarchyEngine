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

public class InternalClient extends Client {
	private String worldJSON;
	private boolean loadingWorld;
	
	public boolean connected;
	
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
								Game.clearServices();
								Load.parseJSON(obj);
								
								// Create connection object
								luaengine.type.object.insts.Connection conInst = new luaengine.type.object.insts.Connection(connection);
								conInst.forceSetName("ConnectionServer");
								conInst.forceSetParent(Game.getService("Connections"));
								
								// Tell server we're all loaded
								InternalGameThread.runLater(()->{
									connection.sendTCP(new ClientConnectFinishTCP());
								});
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
}
