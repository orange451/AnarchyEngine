package luaengine.network.internal;

import java.util.List;

import engine.Game;
import luaengine.type.LuaConnection;
import luaengine.type.object.Instance;
import luaengine.type.object.Service;
import luaengine.type.object.insts.Player;
import luaengine.type.object.services.Players;

public class ClientConnectFinishTCP implements ClientProcessable {
	public long SID; // optional for server
	
	@Override
	public void clientProcess() {
		final long playerId = SID;
		
		Players players = Game.players();

		List<Player> children = players.getPlayers();
		for (int i = 0; i < children.size(); i++) {
			Instance player = children.get(i);
			
			if ( player.getSID() == playerId ) {
				
				// Setup local player stuff
				players.rawset("LocalPlayer", player);
				Game.connections().getLocalConnection().rawset("Player", player);
				player.rawset("Connection", Game.connections().getLocalConnection());
				
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
	}
}
