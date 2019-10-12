package engine.lua.network.internal.protocol;

import com.esotericsoftware.kryonet.Connection;

import engine.Game;
import engine.lua.network.internal.ClientProcessable;
import engine.lua.type.object.Instance;

public class InstanceDestroyTCP implements ClientProcessable {
	public long instanceId;

	public InstanceDestroyTCP() {
		instanceId = -1;
	}
	
	public InstanceDestroyTCP(long instanceId) {
		this.instanceId = instanceId;
	}

	@Override
	public void clientProcess(Connection Connection) {
		Instance instance = Game.getInstanceFromSID(Game.game(), instanceId);
		if ( instance != null )
			instance.destroy();
	}
}
