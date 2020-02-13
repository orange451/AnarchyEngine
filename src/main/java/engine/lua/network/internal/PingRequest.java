/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network.internal;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.network.UUIDSerializable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Connection;

public class PingRequest {
	public long originalSendTime;
	public UUIDSerializable instanceId;
	public boolean ack;
	
	private static final LuaValue C_PING = LuaValue.valueOf("Ping");
	
	public PingRequest() {
		originalSendTime = System.currentTimeMillis();
		ack = false;
	}
	
	public PingRequest(Connection connection) {
		this();
		
		instanceId = new UUIDSerializable(connection.getUUID());
	}
	
	public void process( com.esotericsoftware.kryonet.Connection kryo ) {
		if ( ack ) {
			long ping = System.currentTimeMillis() - originalSendTime;

			Instance inst = Game.getInstanceFromUUID(instanceId.getUUID());
			if ( !Game.isServer() )
				inst = Game.connections().getLocalConnection();
			if ( inst == null || !(inst instanceof Connection) )
				return;
			
			((Connection)inst).rawset(C_PING, LuaValue.valueOf(ping));
			((Connection)inst).notifyPropertySubscribers(C_PING, LuaValue.valueOf(ping));
		} else {
			ack = true;
			
			kryo.sendUDP(this);
		}
		
	}
}
