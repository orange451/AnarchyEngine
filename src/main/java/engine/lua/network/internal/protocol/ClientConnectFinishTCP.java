package engine.lua.network.internal.protocol;

import java.util.List;

import com.esotericsoftware.kryonet.Connection;

import engine.Game;
import engine.lua.network.internal.ClientProcessable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Player;

public class ClientConnectFinishTCP implements ClientProcessable {
	public long SID = -1; // optional for server
	
	@Override
	public void clientProcess(Connection Connection) {
		final long playerId = SID;
		
		// If there already is a local player, destroy it, because the server made a new one!
		if ( SID != -1 ) {
			Player p = Game.players().getLocalPlayer();
			if ( p != null ) {
				p.destroy();
			}
		}

		// Find new server local player
		Instance player = Game.getInstanceFromSID(playerId);
		if ( player == null || !(player instanceof Player) )
			return;
		
		// Setup local player stuff
		Game.players().rawset("LocalPlayer", player);
		engine.lua.type.object.insts.Connection localConnection = Game.connections().getLocalConnection();
		if ( localConnection != null ) {
			localConnection.rawset("Player", player);
			player.rawset("Connection", localConnection);
		}
		
		// Copy starter player scripts in to player
		Instance starterScripts = Game.starterPlayer().starterPlayerScripts();
		List<Instance> cc = starterScripts.getChildren();
		for (int j = 0; j < cc.size(); j++) {
			Instance obj = cc.get(j);
			Instance clo = obj.clone();
			clo.forceSetParent(player.playerScripts());
		}
	}
}
