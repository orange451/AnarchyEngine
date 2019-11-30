package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Vector2;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Vector2Value extends Instance implements TreeViewable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public Vector2Value() {
		super("Vector2Value");
		
		this.defineField(C_VALUE.toString(), new Vector2(), false);
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
	
	public Vector2 getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:(Vector2)value;
	}
	
	public void setValue(Vector2 value) {
		this.set(C_VALUE, value.clone());
	}
}
