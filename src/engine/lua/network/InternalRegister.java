package engine.lua.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.minlog.Log;

import engine.lua.network.internal.ClientConnectFinishTCP;
import engine.lua.network.internal.ClientConnectTCP;
import engine.lua.network.internal.ClientLoadMapTCP;
import engine.lua.network.internal.InstanceCreateTCP;
import engine.lua.network.internal.InstanceDestroyTCP;
import engine.lua.network.internal.InstanceUpdateUDP;
import engine.lua.network.internal.PingRequest;
import engine.lua.network.internal.TestProtocol;

public class InternalRegister {
	public static void register(Kryo kryo) {
		Log.set(Log.LEVEL_WARN);
		
		kryo.register(String.class);
		
		kryo.register(ClientConnectTCP.class);
		kryo.register(ClientConnectFinishTCP.class);
		kryo.register(ClientLoadMapTCP.class);

		kryo.register(TestProtocol.class);
		kryo.register(InstanceCreateTCP.class);
		kryo.register(InstanceDestroyTCP.class);
		kryo.register(InstanceUpdateUDP.class);
		
		kryo.register(PingRequest.class);
	}
}
