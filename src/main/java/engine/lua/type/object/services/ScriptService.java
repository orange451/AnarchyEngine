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

import engine.lua.type.object.SceneStorable;
import engine.lua.type.object.ScriptExecutor;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class ScriptService extends Service implements TreeViewable,ScriptExecutor,SceneStorable {

	public ScriptService() {
		super("ScriptService");
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
		return Icons.icon_script_service;
	}
}
