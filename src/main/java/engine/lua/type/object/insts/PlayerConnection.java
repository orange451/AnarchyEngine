/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;
import engine.lua.network.internal.NonReplicatable;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class PlayerConnection extends Connection implements TreeViewable,NonReplicatable {

	public PlayerConnection( com.esotericsoftware.kryonet.Connection kryoConnection) {
		this();
		this.kryoConnection = kryoConnection;
		this.rawset("Address", LuaValue.valueOf(kryoConnection.getRemoteAddressTCP().getAddress().getHostAddress()));
		
		this.setInstanceable(false);
	}
	
	public PlayerConnection() {
		super("PlayerConnection");
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_network_player;
	}
}
