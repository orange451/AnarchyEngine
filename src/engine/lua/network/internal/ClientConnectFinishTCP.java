package engine.lua.network.internal;

import java.util.List;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Connection;
import engine.lua.type.object.insts.Player;
import engine.lua.type.object.services.Players;

public class ClientConnectFinishTCP implements ClientProcessable {
	public long SID = -1; // optional for server
	
	@Override
	public void clientProcess() {
		final long playerId = SID;
		
		// If there already is a local player, destroy it, because the server made a new one!
		if ( SID != -1 ) {
			Player p = Game.players().getLocalPlayer();
			if ( p != null ) {
				p.destroy();
			}
		}

		// Find new server local player
		Players players = Game.players();
		List<Player> children = players.getPlayers();
		for (int i = 0; i < children.size(); i++) {
			Instance player = children.get(i);
			
			if ( player.getSID() == playerId ) {
				
				// Setup local player stuff
				players.rawset("LocalPlayer", player);
				Connection localConnection = Game.connections().getLocalConnection();
				if ( localConnection != null ) {
					localConnection.rawset("Player", player);
					player.rawset("Connection", localConnection);
				}
				
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
