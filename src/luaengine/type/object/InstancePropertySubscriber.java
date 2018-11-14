package luaengine.type.object;

import org.luaj.vm2.LuaValue;

public interface InstancePropertySubscriber {
	public void onPropertyChange(Instance instance, String property, LuaValue value);
}
