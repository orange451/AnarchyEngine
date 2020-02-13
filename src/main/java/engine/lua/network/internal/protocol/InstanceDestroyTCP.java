/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network.internal.protocol;

import com.esotericsoftware.kryonet.Connection;

import engine.Game;
import engine.lua.network.UUIDSerializable;
import engine.lua.network.internal.ClientProcessable;
import engine.lua.type.object.Instance;

public class InstanceDestroyTCP implements ClientProcessable {
	public long instanceId;
	public UUIDSerializable instanceUUID;

	public InstanceDestroyTCP() {
		instanceId = -1;
	}
	
	public InstanceDestroyTCP(Instance instance) {
		this.instanceUUID = new UUIDSerializable(instance.getUUID());
	}

	@Override
	public void clientProcess(Connection Connection) {
		Instance instance = Game.getInstanceFromUUID(this.instanceUUID.getUUID());
		if ( instance != null )
			instance.destroy();
	}
}
