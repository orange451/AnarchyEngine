package engine.lua.lib;

import java.util.ArrayList;
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
	 * Convert Lua Table into Java List of LuaValue's
	 * @param table
	 * @param filter
	 * @return
	 */
	public static List<LuaValue> tableToList(LuaTable table, Class<?>... filter) {
		List<LuaValue> array = new ArrayList<>();
		item: for (int i = 0; i < table.length(); i++) {
			LuaValue t = table.get(LuaValue.valueOf(i));
			for (int j = 0; j < filter.length; j++) {
				Class<?> c = filter[j];
				if ( !t.getClass().equals(c) && !t.getClass().isAssignableFrom(c) )
					continue item;
			}
			array.add(t);
		}
		
		return array;
	}
	
	
	/**
	 * Convert Lua Table into Java Array of LuaValue's
	 * @param table
	 * @param filter
	 * @return
	 */
	public static LuaValue[] tableToArray(LuaTable table, Class<?>... filter) {
		List<LuaValue> list = tableToList(table, filter);
		return list.toArray(new LuaValue[list.size()]);
	}
}
