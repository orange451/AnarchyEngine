package engine.lua.network.internal.protocol;

import com.esotericsoftware.kryonet.Connection;

import engine.lua.network.internal.ClientProcessable;

public class TestProtocol implements ClientProcessable {
	public String var = "Yay!";
	
	public TestProtocol() {
		var = "Nay! :(";
	}

	@Override
	public void clientProcess(Connection Connection) {
		System.out.println(var);
	}
}
