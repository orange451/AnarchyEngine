/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.lib;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

// Simple read-only table. Raw-setting is permitted.
public class LuaTableReadOnly extends LuaTable {
	public LuaValue setmetatable(LuaValue metatable) { return error("table is read-only"); }
	public void set(int key, LuaValue value) { error("table is read-only"); }
	public LuaValue remove(int pos) { return error("table is read-only"); }
}