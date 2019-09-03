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

import engine.Game;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.Vertex;
import engine.lua.lib.FourArgFunction;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.util.MeshUtils;
import ide.IDEFilePath;
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
	
	public Mesh() {
		super("Mesh");
		
		this.setLocked(false);
		
		this.getmetatable().set(C_CAPSULE, new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius, LuaValue height) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				capsule(radius.tofloat(), height.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(C_SPHERE, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				sphere(radius.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(C_TEAPOT, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue radius) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				teapot(radius.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(C_CUBE, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				if ( !myself.eq_b(Mesh.this) )
					return LuaValue.NIL;
				
				cube(arg2.tofloat());
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(C_BLOCK, new FourArgFunction() {
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
		this.set(C_FILEPATH, C_BLANK);
		this.mesh = mesh;
		this.changed = true;
	}
	
	public BufferedMesh getMesh() {
		LuaValue filePath = this.get(C_FILEPATH);
		
		// Load new mesh
		if ( changed && !filePath.isnil() && filePath.toString().length()>3 && !filePath.toString().equals("nil") ) {
			String path = filePath.toString();
			String realPath = IDEFilePath.convertToSystem(path);
			if ( realPath.contains(".mesh") ) {
				mesh = BufferedMesh.Import(realPath);
			} else {
				AIScene scene = Assimp.aiImportFile(realPath, 0);
				if ( scene == null || scene.mNumMeshes() <= 0 )
					return null;
				
				// Get data
				ArrayList<AIMesh> meshes = new ArrayList<AIMesh>();
				int faceCount = 0;
				for ( int i = 0; i < scene.mMeshes().remaining(); i++ ) {
					AIMesh mm = AIMesh.create(scene.mMeshes().get(i));
					meshes.add( mm );
					faceCount += mm.mNumFaces();
				}

				BufferedMesh bm = new BufferedMesh( faceCount * 3 );
				int vertCounter = 0;
				for ( int i = 0; i < meshes.size(); i++ ) {
					AIMesh mesh = meshes.get(i);
					
					// Get every face in mesh
					org.lwjgl.assimp.AIVector3D.Buffer vertices = mesh.mVertices();
					org.lwjgl.assimp.AIVector3D.Buffer normals = mesh.mNormals();
					org.lwjgl.assimp.AIFace.Buffer faces = mesh.mFaces();
					for (int j = 0; j < mesh.mNumFaces(); j++) {
						AIFace face = faces.get(j);
						IntBuffer indices = face.mIndices();
		
						// Loop through each index
						for (int k = 0; k < indices.capacity(); k++) {
							int index = indices.get(k);
							// Vert Data
							Vector2f textureCoords = new Vector2f();
							Vector3f normalVector = new Vector3f();
		
							// Get the vertex info for this index.
							AIVector3D vertex = vertices.get(index);
							if ( normals != null ) {
								AIVector3D normal = normals.get(index);
								normalVector.set(normal.x(),normal.y(),normal.z());
							}
							if ( mesh.mTextureCoords(0)!=null ) {
								AIVector3D tex = mesh.mTextureCoords(0).get(index);
								textureCoords.set(tex.x(), tex.y());
							}
		
							// Send vertex to output mesh
							Vertex output = new Vertex( vertex.x(), vertex.y(), vertex.z(), normalVector.x, normalVector.y, normalVector.z, textureCoords.x, textureCoords.y, 1, 1, 1, 1 );
							bm.setVertex(vertCounter++, output);
						}
					}
				}
				mesh = bm;
			}
			changed = false;
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

	@Override
	public Icons getIcon() {
		return Icons.icon_mesh;
	}

	@Override
	public Instance getPreferredParent() {
		Service assets = Game.assets();
		if ( assets == null )
			return null;
		
		return assets.findFirstChild(C_MESHES);
	}

	public static String getFileTypes() {
		return "obj,fbx,3ds,dae,ms3d,md5mesh";
	}
}
