package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.Game;
import ide.layout.windows.icons.Icons;
import luaengine.type.object.Instance;
import luaengine.type.object.TreeViewable;

public class Connection extends Instance implements TreeViewable {
	private com.esotericsoftware.kryonet.Connection kryoConnection;
	
	public Connection( com.esotericsoftware.kryonet.Connection kryoConnection) {
		this();
		this.kryoConnection = kryoConnection;
		this.rawset("Address", kryoConnection.getRemoteAddressTCP().getAddress().getHostAddress());
	}
	
	public Connection() {
		super("Connection");
		
		this.defineField("Address", "127.0.0.1", true);
		this.defineField("Data", "", false);
		this.defineField("Player", LuaValue.NIL, true);
		
		this.rawset("Archivable", LuaValue.valueOf(false));
		
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

	@Override
	public Icons getIcon() {
		return Icons.icon_network_player;
	}

	public String getAddress() {
		return this.get("Address").toString();
	}

	public com.esotericsoftware.kryonet.Connection getKryo() {
		return kryoConnection;
	}
}
