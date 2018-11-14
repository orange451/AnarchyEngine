package luaengine.network.internal;

public class TestProtocol implements ClientProcessable {
	public String var = "Yay!";
	
	public TestProtocol() {
		var = "Nay! :(";
	}

	@Override
	public void clientProcess() {
		System.out.println(var);
	}
}
