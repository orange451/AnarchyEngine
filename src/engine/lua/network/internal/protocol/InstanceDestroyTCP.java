package engine.lua.network.internal.protocol;

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
	public void clientProcess() {
		Instance instance = Game.getInstanceFromSID(instanceId);
		if ( instance != null )
			instance.destroy();
	}
}
