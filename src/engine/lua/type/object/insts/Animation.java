package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Animation extends Instance implements TreeViewable {

	public Animation() {
		super("Animation");
		
		this.setLocked(true);
		this.setInstanceable(false);
		
		this.getField("Archivable").setLocked(true);

		this.defineField("Speed", LuaValue.valueOf(1.0), false);
		this.defineField("Looped", LuaValue.valueOf(false), false);
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
		return Icons.icon_film;
	}
}
