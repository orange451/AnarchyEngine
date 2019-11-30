package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Color3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Color3Value extends Instance implements TreeViewable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public Color3Value() {
		super("Color3Value");
		
		this.defineField(C_VALUE.toString(), Color3.white(), false);
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
	
	public Color3 getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:(Color3)value;
	}
	
	public void setValue(Color3 value) {
		this.set(C_VALUE, value.clone());
	}
}
