package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.io.FileResource;
import engine.lua.lib.FourArgFunction;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.util.MeshUtils;
import ide.IDEFilePath;
import ide.layout.windows.icons.Icons;

public class Mesh extends AssetLoadable implements TreeViewable,FileResource {
	private BufferedMesh mesh;
	private boolean changed;
	
	public Mesh() {
		super("Mesh");
		
		this.setLocked(false);
		
		this.getmetatable().set("Capsule", new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius, LuaValue height) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				capsule(radius.tofloat(), height.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("Sphere", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				sphere(radius.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("Teapot", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				teapot(radius.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("Cube", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				cube(arg2.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("Block", new FourArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2, LuaValue arg3, LuaValue arg4) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				block(arg2.tofloat(), arg3.tofloat(), arg4.tofloat());
				return LuaValue.NIL;
			}
		});
	}
	
	public void teapot(float radius) {
		setMesh(MeshUtils.teapot(radius));
	}
	
	public void block(float width, float length, float height) {
		setMesh(MeshUtils.block(width, length, height));
	}
	
	public void cube(float length) {
		setMesh(MeshUtils.cube(length));
	}
	
	public void capsule(float radius, float height) {
		setMesh(MeshUtils.capsule(radius, height, 24));
	}
	
	public void sphere( float radius ) {
		setMesh(MeshUtils.sphere(radius, 24));
	}
	
	public void setMesh(BufferedMesh mesh) {
		this.mesh = mesh;
		this.changed = false;
	}
	
	public BufferedMesh getMesh() {
		LuaValue filePath = this.get("FilePath");
		if ( changed && !filePath.isnil() && filePath.toString().length()>3 && !filePath.toString().equals("nil") ) {
			String path = this.get("FilePath").toString();
			String realPath = IDEFilePath.convertToSystem(path);
			mesh = BufferedMesh.Import(realPath);
			changed = false;
		}
		
		if ( mesh == null )
			return Resources.MESH_SPHERE;
		
		return mesh;
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( this.containsField(key.toString()) ) {
			changed = true;
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

	@Override
	public Icons getIcon() {
		return Icons.icon_mesh;
	}

	@Override
	public Instance getPreferredParent() {
		Service assets = Game.getService("Assets");
		if ( assets == null )
			return null;
		
		return assets.findFirstChild("Meshes");
	}
}
