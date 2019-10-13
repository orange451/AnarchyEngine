package engine.lua.type.object.services;

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
import engine.lua.network.InternalClient;
import engine.lua.network.InternalServer;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Connection;
import ide.layout.windows.icons.Icons;

public class Connections extends Service implements TreeViewable,GameSubscriber {

	private InternalServer internalServer;
	private InternalClient internalClient;
	private boolean enabled;
	
	private static final LuaValue C_DEFAULTPORT = LuaValue.valueOf("DefaultPort");
	private static final LuaValue C_LOCALCONNECTION = LuaValue.valueOf("LocalConnection");
	private static final LuaValue C_ONCONNECT = LuaValue.valueOf("OnConnect");
	private static final LuaValue C_ONDISCONNECT = LuaValue.valueOf("OnDisconnect");
	
	public Connections() {
		super("Connections");
		
		this.defineField(C_DEFAULTPORT.toString(), LuaValue.valueOf(36545), false);
		this.defineField(C_LOCALCONNECTION.toString(), LuaValue.NIL, true);
		
		this.setArchivable(true);
		
		this.rawset(C_ONCONNECT, new LuaEvent());
		this.rawset(C_ONDISCONNECT, new LuaEvent());
		
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
				int portf = Connections.this.get(C_DEFAULTPORT).toint();
				
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
					((LuaEvent)Connections.this.get(C_ONCONNECT)).fire( object );
				}
				return LuaValue.NIL;
			}
		});
		
		((LuaEvent)this.rawget("ChildRemoved")).connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				if ( object instanceof Connection ) {
					((LuaEvent)Connections.this.get(C_ONDISCONNECT)).fire( object );
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
				if ( Game.isServer() && !Game.internalTesting ) {
					int port = Connections.this.get(C_DEFAULTPORT).toint();
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
		LuaValue con = this.get(C_LOCALCONNECTION);
		return !con.isnil()&&con instanceof Connection ? (Connection)con:null;
	}
}
