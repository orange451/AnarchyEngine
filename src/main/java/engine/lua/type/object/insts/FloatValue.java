package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class FloatValue extends Instance implements TreeViewable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public FloatValue() {
		super("FloatValue");
		
		this.defineField(C_VALUE.toString(), LuaValue.valueOf(0.0), false);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_value;
	}
	
	public float getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:value.tofloat();
	}
	
	public void setValue(float value) {
		this.set(C_VALUE, LuaValue.valueOf(value));
	}
}
