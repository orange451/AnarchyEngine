package engine.lua.type.object.insts;

import org.joml.Matrix4f;
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
		this.defineField("Matrix", new Matrix4(), false);
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

	public Matrix4 getMatrix() {
		LuaValue ret = this.get("Matrix");
		return ret==null?null:(Matrix4)ret;
	}
	
	public Matrix4f getMatrixJOML() {
		Matrix4 mat = getMatrix();
		if ( mat == null )
			return null;
		
		return mat.getInternal();
	}
}
