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
import org.luaj.vm2.Varargs;

// Simple read-only table whose contents are initialized from another table.
public class ReadOnlyLuaTableTest extends LuaTable {
	public ReadOnlyLuaTableTest(LuaValue table) {
		presize(table.length(), 0);
		for (Varargs n = table.next(LuaValue.NIL); !n.arg1().isnil(); n = table
				.next(n.arg1())) {
			LuaValue key = n.arg1();
			LuaValue value = n.arg(2);
			super.rawset(key, value.istable() ? new ReadOnlyLuaTableTest(value) : value);
		}
	}
	public LuaValue setmetatable(LuaValue metatable) { return error("table is read-only"); }
	public void set(int key, LuaValue value) { error("table is read-only"); }
	public void rawset(int key, LuaValue value) { error("table is read-only"); }
	public void rawset(LuaValue key, LuaValue value) { error("table is read-only"); }
	public LuaValue remove(int pos) { return error("table is read-only"); }
}