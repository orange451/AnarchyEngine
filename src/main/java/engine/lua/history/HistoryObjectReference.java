package engine.lua.history;

import engine.lua.type.object.Instance;

public class HistoryObjectReference {
	private Instance object;
	private String className;
	private HistoryStack historyStack;
	
	public HistoryObjectReference(HistoryStack historyStack, Instance instance) {
		this.object = instance;
		this.className = instance.getClassName().toString();
	}
	
	public Instance getInstance() {
		if ( object.isDestroyed() ) {
			object = Instance.instance(className);
			historyStack.updateReference(object, this);
		}
		
		return object;
	}
}
