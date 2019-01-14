package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.io.FileResource;
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
				capsule(radius.tofloat(), height.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set("Sphere", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius) {
				sphere(radius.tofloat());
				return LuaValue.NIL;
			}
		});
	}
	
	public void capsule(float radius, float height) {
		setMesh(MeshUtils.capsule(radius, height, 16));
	}
	
	public void sphere( float radius ) {
		setMesh(MeshUtils.sphere(radius, 16));
	}
	
	public void setMesh(BufferedMesh mesh) {
		this.mesh = mesh;
		this.changed = false;
	}
	
	public BufferedMesh getMesh() {
		if ( changed && !this.get("FilePath").isnil() && this.get("FilePath").toString().length()>1 ) {
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
