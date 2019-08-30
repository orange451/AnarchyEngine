package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.gl.mesh.BufferedMesh;
import engine.lua.type.object.Instance;

public class Model extends Instance {

	protected final static LuaValue C_MESH = LuaValue.valueOf("Mesh");
	protected final static LuaValue C_MATERIAL = LuaValue.valueOf("Material");
	protected final static LuaValue C_NAME = LuaValue.valueOf("Name");
	protected final static LuaValue C_PARENT = LuaValue.valueOf("Parent");
	
	public Model() {
		super("Model");

		this.defineField(C_MESH.toString(), LuaValue.NIL, false);
		this.defineField(C_MATERIAL.toString(), LuaValue.NIL, false);
		
		this.setInstanceable(true);
		this.getField(C_NAME).setLocked(false);
		this.getField(C_PARENT).setLocked(false);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		// Parent MUST be inside a prefab
		if ( key.eq_b(C_PARENT))  {
			if ( !(value instanceof Instance) )
				return null;
			if ( !(value instanceof Prefab) )
				return null;
		}
		
		if ( key.eq_b(C_MESH)) {
			if ( !value.isnil() && !(value instanceof Mesh) )
				return null;
		}
		
		if ( key.eq_b(C_MATERIAL) ) {
			if ( !value.isnil() && !(value instanceof Material) )
				return null;
		}
		
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
	
	public BufferedMesh getMesh() {
		return this.get(C_MESH) instanceof Mesh ? ((Mesh)this.get(C_MESH)).getMesh() : null;
	}
	
	public Material getMaterial() {
		LuaValue material = this.get(C_MATERIAL);
		return material.isnil()?null:(Material)material;
	}
	/*
	public engine.gl.MaterialGL getMaterial() {
		return this.get(C_MATERIAL) instanceof Material ? ((Material)this.get(C_MATERIAL)).getMaterial() : null;
	}
	*/
}
