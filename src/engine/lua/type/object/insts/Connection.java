package engine.lua.type.object.insts;

import java.util.concurrent.atomic.AtomicLong;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import engine.lua.network.internal.PingRequest;
import engine.lua.type.object.Instance;

public abstract class Connection extends Instance {
	protected com.esotericsoftware.kryonet.Connection kryoConnection;
	
	public Connection(String objectName) {
		super(objectName);
		this.defineField("Address", LuaValue.valueOf(""), true);
		this.defineField("Data", LuaValue.valueOf(""), false);
		this.defineField("Player", LuaValue.NIL, true);
		this.defineField("Ping", LuaValue.valueOf(0), true);
		
		this.forceSetParent(Game.getService("Connections"));
		
		this.getmetatable().set("Disconnect", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				disconnect();
				return LuaValue.NIL;
			}
		});
		
		this.setInstanceable(false);
		this.setLocked(true);
		
		// Request ping every second
		if (!Game.isRunning())
			return;
		AtomicLong lastSend = new AtomicLong(System.currentTimeMillis());
		
		Game.runLater(()-> {
			Game.runService().heartbeatEvent().connect((args)->{
				if ( System.currentTimeMillis() - lastSend.get() > 100 ) {
					lastSend.set(System.currentTimeMillis());
					kryoConnection.sendUDP(new PingRequest(this));
				}
			});
		});
	}
	
	public com.esotericsoftware.kryonet.Connection getConnection() {
		return kryoConnection;
	}
	
	public Player connectPlayer() {
		// Create new player
		Player p = new Player();
		p.rawset("Connection", Connection.this);
		p.forceSetName(Connection.this.getName());
		
		// Player scripts folder
		Instance ps = new PlayerScripts();
		ps.forceSetParent(p);
		
		// Put in game
		Connection.this.rawset("Player", p);
		p.forceSetParent(Game.getService("Players"));
		
		return p;
	}
	
	public void disconnect() {
		Player player = this.getPlayer();
		if ( player != null )
			player.destroy();
		
		this.destroy();
		
		com.esotericsoftware.kryonet.Connection con = kryoConnection;
		if ( con != null ) {
			kryoConnection = null;
			con.close();
		}
	}

	public Player getPlayer() {
		LuaValue t = this.get("Player");
		return (t.isnil())?null:(Player)t;
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	public String getAddress() {
		return this.get("Address").toString();
	}

	public com.esotericsoftware.kryonet.Connection getKryo() {
		return kryoConnection;
	}
}
