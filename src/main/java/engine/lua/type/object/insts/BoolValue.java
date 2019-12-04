package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class BoolValue extends Instance implements TreeViewable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public BoolValue() {
		super("BoolValue");
		
		this.defineField(C_VALUE.toString(), LuaValue.valueOf(false), false);
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
	
	public boolean getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:value.toboolean();
	}
	
	public void setValue(boolean value) {
		this.set(C_VALUE, LuaValue.valueOf(value));
	}
}
