package engine.lua.type;

import org.luaj.vm2.LuaValue;

public class NumberClamp extends Clamp<Float> {
	private float min;
	private float max;
	
	public NumberClamp(float min, float max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public LuaValue clamp(LuaValue value) {
		return LuaValue.valueOf(Math.min(getMax(), Math.max(getMin(), value.tofloat())));
	}

	@Override
	public Float getMin() {
		return min;
	}

	@Override
	public Float getMax() {
		return max;
	}
}
