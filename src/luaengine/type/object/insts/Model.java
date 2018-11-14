package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.gl.mesh.BufferedMesh;
import luaengine.type.object.Instance;

public class Model extends Instance {
	public Model() {
		super("Model");

		this.defineField("Mesh", LuaValue.NIL, false);
		this.defineField("Material", LuaValue.NIL, false);
		
		this.setInstanceable(true);
		this.getField("Name").setLocked(false);
		this.getField("Parent").setLocked(false);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		// Parent MUST be inside a prefab
		if ( key.toString().equals("Parent") )  {
			if ( !(value instanceof Instance) )
				return null;
			if ( !(value instanceof Prefab) )
				return null;
		}
		
		if ( key.toString().equals("Mesh") ) {
			if ( !value.isnil() && !(value instanceof Mesh) )
				return null;
		}
		
		if ( key.toString().equals("Material") ) {
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
		return this.get("Mesh") instanceof Mesh ? ((Mesh)this.get("Mesh")).getMesh() : null;
	}
	
	public engine.gl.MaterialGL getMaterial() {
		return this.get("Material") instanceof Material ? ((Material)this.get("Material")).getMaterial() : null;
	}
}
