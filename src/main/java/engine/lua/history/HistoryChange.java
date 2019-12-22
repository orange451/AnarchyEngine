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

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;

public class HistoryChange {
	private HistoryObjectReference instanceChanged;
	private LuaValue fieldChanged;
	private LuaValue changedFrom;
	private HistoryObjectReference changedFromInstanceReference;
	private LuaValue changedTo;
	
	public HistoryChange(HistoryObjectReference instance, LuaValue fieldChanged, LuaValue changedFrom, LuaValue changedTo) {
		this.instanceChanged = instance;
		this.fieldChanged = fieldChanged;
		this.changedFrom = changedFrom;
		this.changedTo = changedTo;
		
		if ( changedFrom instanceof Instance ) {
			changedFromInstanceReference = instance.getHistoryStack().getObjectReference((Instance) changedFrom);
		}
		
		this.instanceChanged.update();
	}
	
	public HistoryChange(HistoryStack stack, Instance instance, LuaValue fieldChanged, LuaValue changedFrom, LuaValue changedTo) {
		this(stack.getObjectReference(instance), fieldChanged, changedFrom, changedTo);
	}
	
	public HistoryObjectReference getHistoryInstance() {
		return this.instanceChanged;
	}
	
	public LuaValue getFieldChanged() {
		return this.fieldChanged;
	}
	
	public LuaValue getValueOld() {
		if ( changedFromInstanceReference != null ) {
			return changedFromInstanceReference.getInstance();
		}
		return this.changedFrom;
	}
	
	public LuaValue getValueNew() {
		return this.changedTo;
	}
}
