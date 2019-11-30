package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class ObjectValue extends Instance implements TreeViewable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public ObjectValue() {
		super("ObjectValue");
		
		this.defineField(C_VALUE.toString(), LuaValue.NIL, false);
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
	
	public Instance getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:(Instance)value;
	}
	
	public void setValue(Instance value) {
		this.set(C_VALUE, value);
	}
}
