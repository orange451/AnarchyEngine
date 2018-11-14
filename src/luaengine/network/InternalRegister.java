package luaengine.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.minlog.Log;

import luaengine.network.internal.ClientConnectFinishTCP;
import luaengine.network.internal.ClientConnectTCP;
import luaengine.network.internal.ClientLoadMapTCP;
import luaengine.network.internal.InstanceCreateTCP;
import luaengine.network.internal.InstanceDestroyTCP;
import luaengine.network.internal.InstanceUpdateUDP;
import luaengine.network.internal.PingRequest;
import luaengine.network.internal.TestProtocol;

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
