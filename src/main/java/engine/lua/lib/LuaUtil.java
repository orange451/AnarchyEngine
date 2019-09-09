package engine.lua.lib;

import java.util.List;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class LuaUtil {

	/**
	 * Convert java list to lua table
	 * @param list
	 * @return
	 */
	public static LuaTable listToTable(List<? extends LuaValue> list) {
		LuaTable table = new LuaTable();
		for (int i = 0; i < list.size(); i++) {
			table.set(i+1, (LuaValue) list.get(i));
		}
		
		return table;
	}

	/**
	 * Convert java array to lua table. Elements in array MUST extend LuaValue. Unchecked.
	 * @param objects
	 * @return
	 */
	public static LuaTable arrayToTable(Object[] objects) {
		LuaTable table = new LuaTable();
		for (int i = 0; i < objects.length; i++) {
			table.set(i+1, (LuaValue) objects[i]);
		}
		
		return table;
	}

	/**
	 * Converts JSON object into lua table.
	 * @param obj
	 * @return
	 */
	public static LuaValue jsonToTable(JSONObject obj) {
		return new LuaTable();
	}

	/**
	 * Converts lua table to JSON
	 * @param data
	 * @return
	 */
	public static JSONObject tableToJson(LuaTable data) {
		return new JSONObject();
	}
}
