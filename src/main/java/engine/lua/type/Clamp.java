package engine.lua.type;

import org.luaj.vm2.LuaValue;

public abstract class Clamp<T> {
	public abstract LuaValue clamp(LuaValue value);
	public abstract T getMin();
	public abstract T getMax();
}
