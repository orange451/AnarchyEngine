package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Matrix4Value extends Instance implements TreeViewable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public Matrix4Value() {
		super("Matrix4Value");
		
		this.defineField(C_VALUE.toString(), new Matrix4(), false);
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
	
	public Matrix4 getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:(Matrix4)value;
	}
	
	public void setValue(Matrix4 value) {
		this.set(C_VALUE, value.clone());
	}
}
