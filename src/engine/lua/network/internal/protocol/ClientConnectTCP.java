package engine.lua.network.internal.protocol;

public class ClientConnectTCP {
	public final String username;
	public final String version;
	
	public ClientConnectTCP() {
		username = "Test";
		version = "";
	}
	
	public ClientConnectTCP(String username, String version) {
		this.username = username;
		this.version = version;
	}

}
