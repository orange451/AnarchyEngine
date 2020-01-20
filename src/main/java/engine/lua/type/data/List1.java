/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.network.internal.JSONUtil;
import engine.lua.type.LuaValuetype;

public class List1 extends LuaValuetype {

	private List<LuaValue> internal;
	
	public List1() {
		this.internal = new ArrayList<>();
		
		this.getmetatable().set("Size", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf(size());
			}
		});
		
		this.getmetatable().set("Clear", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				clear();
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("Add", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue object1) {
				addElement(object1);
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("Remove", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue object1) {
				removeElement(object1);
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("Get", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue object1) {
				if ( !object1.isnumber() )
					return LuaValue.NIL;
				return getElement(object1.checkint());
			}
		});
	}
	
	public List1(List<LuaValue> list) {
		this();
		
		for (int i = 0; i < list.size(); i++) {
			LuaValue element = list.get(i);
			LuaValue newElement = null;
			
			if ( element instanceof LuaValuetype )
				newElement = ((LuaValuetype)element).clone();
			
			try {
				Method m = element.getClass().getDeclaredMethod("clone");
				newElement = (LuaValue) m.invoke(element);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			if ( newElement == null )
				continue;
			this.addElement( newElement );
		}
	}
	
	@Override
	protected void onRegister(LuaTable table) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected LuaValue newInstanceFunction() {
		return new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				int amtarg = args.narg();
				
				try {
					if ( amtarg == 0 ) {
						return new List1();
					} else {
						List1 arr = new List1();
						for (int i = 0; i < amtarg; i++) {
							arr.addElement(args.arg(i+1));
						}
						return arr;
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				return LuaValue.NIL;
			}
		};
	}

	/**
	 * Add an element to the internal array
	 * @param arg
	 */
	public void addElement(LuaValue arg) {
		internal.add(arg);
	}
	
	/**
	 * Remove an element from the internal array.
	 * @param arg
	 */
	public void removeElement(LuaValue arg) {
		internal.remove(arg);
	}
	
	/**
	 * Returns the amount of elements in the internal array.
	 * @return
	 */
	public int size() {
		return internal.size();
	}
	
	/**
	 * Clears all elements from the list.
	 */
	public void clear() {
		this.internal.clear();
	}
	
	/**
	 * Returns the element at a specific index.
	 * @param index
	 * @return
	 */
	public LuaValue getElement(int index) {
		return internal.get(index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSON() {
		JSONObject ret = new JSONObject();
		JSONArray arr = new JSONArray();
		
		for (int i = 0; i < internal.size(); i++) {
			LuaValue luaValue = internal.get(i);
			Object val = JSONUtil.serializeField(luaValue);
			if ( val == null )
				continue;
			
			arr.add(val);
		}
		
		ret.put("ArrayData", arr);
		return ret;
	}


	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		
		for (int i = 0; i < internal.size(); i++) {
			builder.append(internal.get(i).tostring());
			if ( i < internal.size()-1 )
				builder.append(", ");
		}
		
		builder.append("]");
		
		return builder.toString();
	}
	
	public LuaValue tostring() {
		return LuaValue.valueOf(typename()+":"+toString());
	}
	
	public static List1 fromJSON(JSONObject json) {
		List1 ret = new List1();
		List<JSONObject> values = (List<JSONObject>) json.get("ArrayData");
		for (int i = 0; i < values.size(); i++) {
			LuaValue val = JSONUtil.deserializeField(values.get(i));
			if ( val == null )
				continue;
			
			ret.addElement(val);
		}
		
		return ret;
	}

	@Override
	public String typename() {
		return "List1";
	}

	@Override
	public LuaValuetype fromString(String input) {
		if ( input.startsWith("[") )
			input = input.substring(1);
		if ( input.endsWith("]") )
			input = input.substring(0, input.length()-1);

		this.internal.clear();
		
		String[] str = input.split(",");
		for (int i = 0; i < str.length; i++) {
			addElement(LuaValue.valueOf(str[i]));
		}
		
		return this;
	}

	@Override
	public LuaValuetype clone() {
		return new List1(internal);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

}
