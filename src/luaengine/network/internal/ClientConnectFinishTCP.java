package luaengine.network.internal;

import java.util.List;

import engine.Game;
import luaengine.type.LuaConnection;
import luaengine.type.object.Instance;
import luaengine.type.object.Service;

public class ClientConnectFinishTCP implements ClientProcessable {
	public long SID; // optional for server

	private LuaConnection con;
	@Override
	public void clientProcess() {
		final long playerId = SID;
		
		Service players = Game.getService("Players");
		con = Game.runService().getHeartbeatEvent().connect((args)-> {
			List<Instance> children = players.getChildren();
			for (int i = 0; i < children.size(); i++) {
				Instance player = children.get(i);
				if ( player.getSID() == playerId ) {
					players.rawset("LocalPlayer", player);
					con.disconnect();
					
					// Copy starter player scripts in to player
					Instance starterScripts = Game.getService("Storage").findFirstChild("StarterPlayerScripts");
					List<Instance> cc = starterScripts.getChildren();
					for (int j = 0; j < cc.size(); j++) {
						Instance obj = cc.get(j);
						Instance clo = obj.clone();
						clo.forceSetParent(player.findFirstChild("PlayerScripts"));
					}
					return;
				}
			}
		});
	}
}
