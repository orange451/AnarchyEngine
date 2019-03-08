package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class AnimationKeyframe extends Instance implements TreeViewable {

	public AnimationKeyframe() {
		super("AnimationKeyframe");
		
		this.setLocked(true);
		this.setInstanceable(false);
		
		this.getField("Archivable").setLocked(true);
		
		this.defineField("Bone", LuaValue.NIL, true);
		this.defineField("Matrix", new Matrix4(), true);
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
