package engine.lua.type.object.insts;

import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class IntValue extends Instance implements TreeViewable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public IntValue() {
		super("IntValue");
		
		this.defineField(C_VALUE.toString(), LuaValue.valueOf(0), false);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.equals(C_VALUE) && !value.isint() )
			value = LuaInteger.valueOf(value.toint());
		
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
	
	public int getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:value.toint();
	}
	
	public void setValue(int value) {
		this.set(C_VALUE, LuaInteger.valueOf(value));
	}
}
