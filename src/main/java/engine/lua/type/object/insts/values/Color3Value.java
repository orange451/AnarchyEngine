/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.values;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Color3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Color3Value extends ValueBase implements TreeViewable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public Color3Value() {
		super("Color3Value");
		
		this.defineField(C_VALUE.toString(), Color3.white(), false);
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
		return Icons.icon_value;
	}
	
	public Color3 getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:(Color3)value;
	}
	
	public void setValue(Color3 value) {
		this.set(C_VALUE, value.clone());
	}
}
