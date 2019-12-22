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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			table.set(i+1, list.get(i));
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
	 * @param <T>
	 * @param table
	 * @param filter
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> tableToList(LuaTable table, Class<?>... filter) {
		List<LuaValue> array = new ArrayList<>();
		LuaValue[] keys = table.keys();
		for (int i = 0; i < keys.length; i++) {
			LuaValue t = table.get(keys[i]);
			boolean contains = filter.length==0?true:false;
			for (int j = 0; j < filter.length; j++) {
				Class<?> c = filter[j];
				if ( t.getClass().equals(c) || c.isAssignableFrom(t.getClass()) )
					contains = true;
			}
			
			if ( contains )
				array.add(t);
		}
		
		return (List<T>) array;
	}
	
	
	/**
	 * Convert Lua Table into Java Array of LuaValue's
	 * @param table
	 * @param filter
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] tableToArray(LuaTable table, Class<?>... filter) {
		List<T> list = tableToList(table, filter);
		return (T[]) list.toArray(new Object[list.size()]);
	}

	/**
	 * Convert LuaTable to JSONObject. Keys are converted to Strings
	 * @param data
	 * @return
	 */
	public static JSONObject tableToJson(LuaTable data) {
		JSONObject json = new JSONObject();
		
		LuaValue[] keys = data.keys();
		for (int i = 0; i < keys.length; i++) {
			LuaValue key = keys[i];
			json.put(key.toString(), data.get(key));
		}
		
		return json;
	}

	/**
	 * convert json object to LuaTable. JSON Object values should all extend LuaValue in some way. Keys should be strings.
	 * @param obj
	 * @return
	 */
	public static LuaTable jsonToTable(JSONObject obj) {
		LuaTable table = new LuaTable();
		
		Set<?> entryset = obj.entrySet();
		Iterator<?> iterator = entryset.iterator();
		while(iterator.hasNext()) {
			Map.Entry entry = (Map.Entry)iterator.next();
			table.set(LuaValue.valueOf(entry.getKey().toString()), (LuaValue)entry.getValue());
		}
		
		
		return table;
	}
}
