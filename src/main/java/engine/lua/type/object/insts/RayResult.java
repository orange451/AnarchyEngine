package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class RayResult extends Instance implements TreeViewable {

	protected static final LuaValue C_OBJECT = LuaValue.valueOf("Object");
	protected static final LuaValue C_POSITION = LuaValue.valueOf("Position");
	protected static final LuaValue C_NORMAL = LuaValue.valueOf("Normal");
	
	public RayResult() {
		super("RayResult");

		this.defineField(C_OBJECT.toString(), LuaValue.NIL, true);
		this.defineField(C_POSITION.toString(), new Vector3(), true);
		this.defineField(C_NORMAL.toString(), new Vector3(), true);
		
		this.setInstanceable(false);
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
}
