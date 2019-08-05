package engine.lua.network;

import com.esotericsoftware.kryo.Kryo;

import engine.lua.network.internal.PingRequest;
import engine.lua.network.internal.protocol.ClientConnectFinishTCP;
import engine.lua.network.internal.protocol.ClientConnectTCP;
import engine.lua.network.internal.protocol.ClientLoadMapTCP;
import engine.lua.network.internal.protocol.InstanceCreateTCP;
import engine.lua.network.internal.protocol.InstanceDestroyTCP;
import engine.lua.network.internal.protocol.InstanceUpdateUDP;
import engine.lua.network.internal.protocol.TestProtocol;

public class InternalRegister {
	public static void register(Kryo kryo) {
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
