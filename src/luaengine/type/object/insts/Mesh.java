package luaengine.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.io.FileResource;
import ide.IDEFilePath;
import ide.layout.windows.icons.Icons;
import luaengine.type.object.AssetLoadable;
import luaengine.type.object.Instance;
import luaengine.type.object.Service;
import luaengine.type.object.TreeViewable;

public class Mesh extends AssetLoadable implements TreeViewable,FileResource {
	private BufferedMesh mesh;
	private boolean changed;
	
	public Mesh() {
		super("Mesh");
		
		this.setLocked(false);
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
