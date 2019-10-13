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
		
		LuaValue[] fields = object.getFields();
		for (int i = 0; i < fields.length; i++) {
			fieldData.put(fields[i], object.get(fields[i]));
		}
	}
}
