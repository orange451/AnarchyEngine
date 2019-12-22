/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class StarterPlayer extends Service implements TreeViewable {

	private static final LuaValue C_STARTERPLAYERSCRIPTS = LuaValue.valueOf("StarterPlayerScripts");
	
	public StarterPlayer() {
		super("StarterPlayer");
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_starter_player;
	}

	public Instance starterPlayerScripts() {
		return findFirstChild(C_STARTERPLAYERSCRIPTS);
	}
}
