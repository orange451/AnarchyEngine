package luaengine.lib;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

// Simple read-only table. Raw-setting is permitted.
public class LuaTableReadOnly extends LuaTable {
	public LuaValue setmetatable(LuaValue metatable) { return error("table is read-only"); }
	public void set(int key, LuaValue value) { error("table is read-only"); }
	public LuaValue remove(int pos) { return error("table is read-only"); }
}