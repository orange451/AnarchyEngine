package luaengine.network.internal;

import org.luaj.vm2.LuaValue;

import engine.Game;
import luaengine.type.object.Instance;
import luaengine.type.object.insts.Connection;

public class PingRequest {
	public long originalSendTime;
	public long instanceId;
	public boolean ack;
	
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

			Instance inst = Game.getInstanceFromSID(instanceId);
			if ( !Game.isServer() )
				inst = Game.connections().getLocalConnection();
			if ( inst == null || !(inst instanceof Connection) )
				return;
			
			((Connection)inst).rawset("Ping", ping);
			((Connection)inst).notifyPropertySubscribers("Ping", LuaValue.valueOf(ping));
		} else {
			ack = true;
			
			kryo.sendUDP(this);
		}
		
	}
}
