package engine.lua.network.internal;

import com.esotericsoftware.kryonet.Connection;

public interface ClientProcessable {
	public void clientProcess(Connection Connection);
}
