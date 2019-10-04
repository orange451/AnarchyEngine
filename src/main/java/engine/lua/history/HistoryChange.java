package engine.lua.history;

import org.luaj.vm2.LuaValue;

public class HistoryChange {
	private HistoryObjectReference instanceChanged;
	private LuaValue fieldChanged;
	private LuaValue changedFrom;
	private LuaValue changedTo;
	
	public HistoryChange(HistoryObjectReference instance, LuaValue fieldChanged, LuaValue changedFrom, LuaValue changedTo) {
		this.instanceChanged = instance;
		this.fieldChanged = fieldChanged;
		this.changedFrom = changedFrom;
		this.changedTo = changedTo;
		
		this.instanceChanged.update();
	}
	
	public HistoryObjectReference getInstance() {
		return this.instanceChanged;
	}
	
	public LuaValue getFieldChanged() {
		return this.fieldChanged;
	}
	
	public LuaValue getValueOld() {
		return this.changedFrom;
	}
	
	public LuaValue getValueNew() {
		return this.changedTo;
	}
}
