/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network;

import com.esotericsoftware.kryo.Kryo;

import engine.lua.network.internal.PingRequest;
import engine.lua.network.internal.protocol.ClientConnectFinishTCP;
import engine.lua.network.internal.protocol.ClientConnectTCP;
import engine.lua.network.internal.protocol.ClientLoadMapTCP;
import engine.lua.network.internal.protocol.InstanceCreateTCP;
import engine.lua.network.internal.protocol.InstanceDestroyTCP;
import engine.lua.network.internal.protocol.InstanceUpdateUDP;
import engine.lua.network.internal.protocol.TestProtocol;

public class InternalRegister {
	public static void register(Kryo kryo) {
		kryo.register(String.class);
		kryo.register(UUIDSerializable.class);
		
		kryo.register(ClientConnectTCP.class);
		kryo.register(ClientConnectFinishTCP.class);
		kryo.register(ClientLoadMapTCP.class);

		kryo.register(TestProtocol.class);
		kryo.register(InstanceCreateTCP.class);
		kryo.register(InstanceDestroyTCP.class);
		kryo.register(InstanceUpdateUDP.class);
		
		kryo.register(PingRequest.class);
	}
}
