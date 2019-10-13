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
