package engine.lua.network.internal.protocol;

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
		
		((Player)player).start();
	}
}
