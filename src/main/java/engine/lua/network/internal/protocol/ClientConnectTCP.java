package engine.lua.network.internal.protocol;

public class ClientConnectTCP {
	public final String username;
	public final String version;
	public final String data;
	
	public ClientConnectTCP() {
		username = "Test";
		version = "";
		data = "";
	}
	
	public ClientConnectTCP(String username, String version, String data) {
		this.username = username;
		this.version = version;
		this.data = data;
	}

}
