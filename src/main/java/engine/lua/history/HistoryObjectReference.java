/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.history;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;

public class HistoryObjectReference {
	private Instance object;
	private String className;
	private HistoryStack historyStack;
	
	private HashMap<LuaValue, LuaValue> fieldData = new HashMap<>();
	
	public HistoryObjectReference(HistoryStack historyStack, Instance instance) {
		this.object = instance;
		this.className = instance.getClassName().toString();
		this.historyStack = historyStack;
	}
	
	public HistoryStack getHistoryStack() {
		return this.historyStack;
	}
	
	public Instance getInstance() {
		if ( object == null || object.isDestroyed() ) {
			object = Instance.instance(className);
			historyStack.updateReference(object, this);
			
			Set<Entry<LuaValue, LuaValue>> set = fieldData.entrySet();
			Iterator<Entry<LuaValue, LuaValue>> iterator = set.iterator();
			while (iterator.hasNext()) {
				Entry<LuaValue, LuaValue> entry = iterator.next();
				object.forceset(entry.getKey(), entry.getValue());
			}
		}
		
		return object;
	}
	
	protected void update() {
		if ( object.isDestroyed() )
			return;
		
		LuaValue[] fields = object.getFieldNames();
		for (int i = 0; i < fields.length; i++) {
			fieldData.put(fields[i], object.get(fields[i]));
		}
	}
}
