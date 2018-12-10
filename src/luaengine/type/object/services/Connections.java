package luaengine.type.object.services;

import java.util.ArrayList;
import java.util.List;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import engine.Game;
import engine.GameSubscriber;
import ide.layout.windows.icons.Icons;
import luaengine.network.InternalClient;
import luaengine.network.InternalServer;
import luaengine.type.LuaEvent;
import luaengine.type.object.Instance;
import luaengine.type.object.Service;
import luaengine.type.object.TreeViewable;
import luaengine.type.object.insts.Connection;
import luaengine.type.object.insts.GameObject;

public class Connections extends Service implements TreeViewable,GameSubscriber {

	private InternalServer internalServer;
	private InternalClient internalClient;
	private boolean enabled;
	public List<GameObject> ownedCharacters;
	
	public Connections() {
		super("Connections");
		
		ownedCharacters = new ArrayList<GameObject>();
		
		this.defineField("DefaultPort", LuaValue.valueOf(36545), false);
		this.defineField("LocalConnection", LuaValue.NIL, true);
		
		this.rawset("Archivable", LuaValue.valueOf(true));
		this.rawset("OnConnect", new LuaEvent());
		this.rawset("OnDisconnect", new LuaEvent());
		
		this.getmetatable().set("ConnectTo", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				LuaValue username = args.arg(2);
				LuaValue ip = args.arg(3);
				LuaValue data = args.arg(4);
				
				if ( username.isnil() ) {
					LuaValue.error("ConnectTo requires a specified username");
					return LuaValue.NIL;
				}
				if ( ip.isnil() )
					ip = LuaValue.valueOf("127.0.0.1");
				if ( data.isnil() )
					data = new LuaTable();
				if ( !(data instanceof LuaTable) ) {
					LuaValue.error("data field must be of type table, or nil.");
					return LuaValue.NIL;
				}
				
				String ipf = ip.toString();
				int portf = Connections.this.get("DefaultPort").toint();
				
				return LuaValue.valueOf(connect(ipf, portf, username.toString(), (LuaTable) data));
			}
		});
		
		this.getmetatable().set("GetConnectionFromIP", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue ip) {
				Connection c = getConnectionFromIP(ip.toString());
				if ( c == null )
					return LuaValue.NIL;
				
				return c;
			}
		});
		
		((LuaEvent)this.rawget("ChildAdded")).connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				if ( object instanceof Connection ) {
					((LuaEvent)Connections.this.get("OnConnect")).fire( object );
				}
				return LuaValue.NIL;
			}
		});
		
		((LuaEvent)this.rawget("ChildRemoved")).connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				if ( object instanceof Connection ) {
					((LuaEvent)Connections.this.get("OnDisconnect")).fire( object );
				}
				return LuaValue.NIL;
			}
		});
		
		Game.getGame().subscribe(this);
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
	public Icons getIcon() {
		return Icons.icon_network;
	}
	
	@Override
	public void onDestroy() {
		Game.getGame().unsubscribe(this);
		stop();
	}
	
	private void stop() {
		enabled = false;
		if ( internalServer != null ) {
			internalServer.stop();
		}
		if ( internalClient != null ) {
			internalClient.stop();
		}
	}
	
	private boolean connect(String ip, int port, String username, LuaTable data) {
		internalClient = new InternalClient(ip, port, username, data);
		return internalClient.connected;
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		if ( !important )
			return;
		if ( !Game.isRunning() ) {
			if ( enabled ) {
				stop();
			}
		} else {
			if ( !enabled ) {
				enabled = true;
				if ( Game.isServer() || Game.internalTesting ) {
					int port = Connections.this.get("DefaultPort").toint();
					internalServer = new InternalServer(port);
				}
			}
		}
	}

	public Connection getConnectionFromIP(String hostAddress) {
		List<Instance> instances = this.getChildren();
		for (int i = 0; i < instances.size(); i++) {
			Instance inst = instances.get(i);
			if ( !(inst instanceof Connection) )
				continue;
			
			if ( ((Connection)inst).getAddress().equals(hostAddress) )
				return (Connection) inst;
		}
		
		return null;
	}

	public Connection getConnectionFromKryo(com.esotericsoftware.kryonet.Connection connection) {
		List<Instance> instances = this.getChildren();
		for (int i = 0; i < instances.size(); i++) {
			Instance inst = instances.get(i);
			if ( !(inst instanceof Connection) )
				continue;
			
			com.esotericsoftware.kryonet.Connection kryo = ((Connection)inst).getKryo();
			
			if ( kryo != null && kryo.equals(connection) )
				return (Connection) inst;
		}
		
		return null;
	}

	public List<Connection> getConnections() {
		List<Connection> cons = new ArrayList<Connection>();
		List<Instance> c = this.getChildren();
		for (int i = 0; i < c.size(); i++) {
			Instance inst = c.get(i);
			if ( inst instanceof Connection ) {
				cons.add((Connection) inst);
			}
		}
		
		return cons;
	}

	public Connection getLocalConnection() {
		LuaValue con = this.get("LocalConnection");
		return !con.isnil()&&con instanceof Connection ? (Connection)con:null;
	}
}
