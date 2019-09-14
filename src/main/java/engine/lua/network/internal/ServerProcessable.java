package engine.lua.network.internal;

import com.esotericsoftware.kryonet.Connection;

public interface ServerProcessable {
	public void serverProcess(Connection Connection);
}
