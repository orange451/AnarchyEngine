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

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class CSS extends Instance implements TreeViewable {
	
	private static final LuaValue C_SOURCE = LuaValue.valueOf("Source");
	
	public CSS() {
		super("CSS");
		
		this.defineField(C_SOURCE.toString(), LuaValue.valueOf("/* CSS Document */"), false);
	}

	public String getSource() {
		return this.get(C_SOURCE).toString();
	}

	public void setSource(String text) {
		this.set(C_SOURCE, LuaValue.valueOf(text));
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
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_script_css;
	}
}
