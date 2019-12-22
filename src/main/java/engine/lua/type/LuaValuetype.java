/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import engine.lua.LuaEngine;

public abstract class LuaValuetype extends LuaDatatype {
	public static HashMap<String, Class<? extends LuaValuetype>> DATA_TYPES = new HashMap<String,Class<? extends LuaValuetype>>();

	public LuaValuetype() {
		LuaTable table = new LuaTable();
		table.set(LuaValue.INDEX, table);
		this.setmetatable(table);
		
		if ( LuaEngine.globals.get(typename()).isnil() ) {
			register(this);
		}
	}

	public static void register(LuaValuetype type) {
		LuaTable table = new LuaTable();
		table.set("new", type.newInstanceFunction());
		table.set(LuaValue.INDEX, table);
		type.onRegister(table);
		LuaEngine.globals.set(type.typename(), table);
		
		DATA_TYPES.put(type.typename(), type.getClass());
	}
	
	public LuaValue tostring() {
		return LuaValue.valueOf(this.toString());
	}
	
	protected abstract void onRegister(LuaTable table);
	protected abstract LuaValue newInstanceFunction();
	public abstract JSONObject toJSON();
	public abstract LuaValuetype fromString(String input);
	
	public abstract LuaValuetype clone();
}
