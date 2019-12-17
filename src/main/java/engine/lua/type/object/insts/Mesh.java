package engine.lua.type.object.insts;

import java.nio.IntBuffer;
import java.util.ArrayList;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

import engine.FilePath;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.Vertex;
import engine.lua.lib.FourArgFunction;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.TreeViewable;
import engine.util.MeshUtils;
import ide.layout.windows.icons.Icons;

public class Mesh extends AssetLoadable implements TreeViewable {
	private BufferedMesh mesh;
	private boolean changed;

	private static final LuaValue C_CAPSULE = LuaValue.valueOf("Capsule");
	private static final LuaValue C_SPHERE = LuaValue.valueOf("Sphere");
	private static final LuaValue C_TEAPOT = LuaValue.valueOf("Teapot");
	private static final LuaValue C_CUBE = LuaValue.valueOf("Cube");
	private static final LuaValue C_BLOCK = LuaValue.valueOf("Block");
	
	private static final LuaValue C_MESHES = LuaValue.valueOf("Meshes");
	private static final LuaValue C_BLANK = LuaValue.valueOf("");
	
	private static final LuaValue C_MESHLOADED = LuaValue.valueOf("MeshLoaded");
	
	public Mesh() {
		super("Mesh");
		
		this.setLocked(false);
		
		this.getmetatable().set(C_CAPSULE.toString(), new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius, LuaValue height) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				capsule(radius.tofloat(), height.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(C_SPHERE.toString(), new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				sphere(radius.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(C_TEAPOT.toString(), new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				teapot(radius.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(C_CUBE.toString(), new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				cube(arg2.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(C_BLOCK.toString(), new FourArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2, LuaValue arg3, LuaValue arg4) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				block(arg2.tofloat(), arg3.tofloat(), arg4.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.rawset(C_MESHLOADED, new LuaEvent());
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
		this.set(C_FILEPATH, C_BLANK);
		this.mesh = mesh;
		this.changed = true;
		this.meshLoaded().fire();
	}
	
	public BufferedMesh getMesh() {
		LuaValue filePath = this.get(C_FILEPATH);
		
		// Load new mesh
		if ( changed && !filePath.isnil() && filePath.toString().length()>3 && !filePath.toString().equals("nil") ) {
			String path = filePath.toString();
			String realPath = FilePath.convertToSystem(path);
			if ( realPath.endsWith(".mesh") ) {
				mesh = BufferedMesh.Import(realPath);
			} else {
				mesh = MeshUtils.Import(realPath);
			}
			changed = false;
			this.meshLoaded().fire();
		}
		
		if ( mesh == null )
			return Resources.MESH_SPHERE;
		
		return mesh;
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( this.containsField(key) ) {
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
	
	public LuaEvent meshLoaded() {
		return (LuaEvent) this.get(C_MESHLOADED);
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_mesh;
	}

	@Override
	public LuaValue getPreferredParent() {
		return C_MESHES;
	}

	public static String getFileTypes() {
		return "x,obj,fbx,3ds,smd,xml,dae,gltf,ms3d,blend,md5mesh";
	}
}
