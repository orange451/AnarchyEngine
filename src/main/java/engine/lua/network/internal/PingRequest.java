package engine.lua.network.internal;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Connection;

public class PingRequest {
	public long originalSendTime;
	public long instanceId;
	public boolean ack;
	
	private static final LuaValue C_PING = LuaValue.valueOf("Ping");
	
	public PingRequest() {
		originalSendTime = System.currentTimeMillis();
		ack = false;
	}
	
	public PingRequest(Connection connection) {
		this();
		
		instanceId = connection.getSID();
	}
	
	public void process( com.esotericsoftware.kryonet.Connection kryo ) {
		if ( ack ) {
			long ping = System.currentTimeMillis() - originalSendTime;

			Instance inst = Game.getInstanceFromSID(Game.game(), instanceId);
			if ( !Game.isServer() )
				inst = Game.connections().getLocalConnection();
			if ( inst == null || !(inst instanceof Connection) )
				return;
			
			((Connection)inst).rawset(C_PING, LuaValue.valueOf(ping));
			((Connection)inst).notifyPropertySubscribers(C_PING, LuaValue.valueOf(ping));
		} else {
			ack = true;
			
			kryo.sendUDP(this);
		}
		
	}
}
