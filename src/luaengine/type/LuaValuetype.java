package luaengine.type;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import luaengine.LuaEngine;

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
	
	protected abstract void onRegister(LuaTable table);
	protected abstract LuaValue newInstanceFunction();
	public abstract JSONObject toJSON();
	public abstract LuaValuetype fromString(String input);
	
	public abstract LuaValuetype clone();
}
