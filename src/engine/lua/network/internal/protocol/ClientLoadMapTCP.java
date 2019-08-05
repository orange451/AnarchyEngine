package engine.lua.network.internal.protocol;

public class ClientLoadMapTCP {
	public String data;
	public boolean finished;
	
	public ClientLoadMapTCP() {
		//
	}
	
	public ClientLoadMapTCP(String data) {
		this.data = data;
	}
	
	public ClientLoadMapTCP(boolean finished) {
		this.finished = true;
	}
}
