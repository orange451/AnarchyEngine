package engine.lua.type.object;

import org.luaj.vm2.LuaValue;

public interface InstancePropertySubscriber {
	public void onPropertyChange(Instance instance, LuaValue key, LuaValue value);
}
