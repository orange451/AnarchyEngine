package engine.lua.type.object.services;

import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMaterialProperty;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.Vertex;
import engine.lua.type.data.Color3;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.AnimationData;
import engine.lua.type.object.insts.AssetFolder;
import engine.lua.type.object.insts.Bones;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.Texture;
import engine.util.FileUtils;
import engine.util.IOUtil;
import ide.layout.windows.icons.Icons;

public class Assets extends Service implements TreeViewable {

	public Assets() {
		super("Assets");
		
		this.getmetatable().set("ImportMesh", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue path) {
				Mesh m = new Mesh();
				m.set("FilePath", path.toString());
				m.forceSetParent(m.getPreferredParent());
				return m;
			}
		});
		
		this.getmetatable().set("ImportTexture", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg3) {
				return importTexture(arg3.toString());
			}
		});
		
		this.getmetatable().set("ImportPrefab", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue path) {
				return importPrefab(path.toString());
			}
		});
		
		this.getmetatable().set("NewMesh", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Mesh t = new Mesh();
				t.forceSetParent(t.getPreferredParent());
				return t;
			}
		});
		
		this.getmetatable().set("NewTexture", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Texture t = new Texture();
				t.forceSetParent(t.getPreferredParent());
				return t;
			}
		});
		
		this.getmetatable().set("NewMaterial", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Material m = new Material();
				m.forceSetParent(m.getPreferredParent());
				return m;
			}
		});
		
		this.getmetatable().set("NewPrefab", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Prefab p = new Prefab();
				p.forceSetParent(p.getPreferredParent());
				return p;
			}
		});
	}
	
	public Texture importTexture(String filepath) {
		Texture t = new Texture();
		t.setFilePath(filepath);
		t.forceSetParent(t.getPreferredParent());
		return t;
	}

	public Prefab importPrefab(String filePath) {
		return importPrefab(filePath, 1, -1);
	}

	public void newPackage(String name) {
		AssetFolder folder = new AssetFolder();
		folder.rawset("Name", name);
		folder.forceSetParent(this);
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
	public Icons getIcon() {
		return Icons.icon_box;
	}
	
	private List<AssetLoadable> getAssets(Instance directory) {
		List<AssetLoadable> ret = new ArrayList<AssetLoadable>();
		List<Instance> c = directory.getChildren();
		
		for (int i = 0; i < c.size(); i++) {
			Instance in = c.get(i);
			if ( in instanceof AssetLoadable ) {
				ret.add((AssetLoadable) in);
			}
		}
		return ret;
	}
	
	/**
	 * Get list of all textures loaded
	 * @return
	 */
	public List<AssetLoadable> getTextures() {
		return getAssets((Instance)this.getChildrenWithName("Textures").get(0));
	}
	
	/**
	 * Get list of all meshes loaded
	 * @return
	 */
	public List<AssetLoadable> getMeshes() {
		return getAssets((Instance)this.getChildrenWithName("Meshes").get(0));
	}
	
	//String fileName = Thread.currentThread().getContextClassLoader().getResource("model.obj").getFile();
	/**
	 * Import a static-model using assimp.
	 * @param filePath
	 * @return
	 */
	private static Prefab importPrefab(String filePath, float scale, int extraFlags) {
		String specificFile = FileUtils.getFileNameFromPath(filePath);
		Prefab prefab = new Prefab();
		
		try {
			// Get File
			System.out.println("Loading assimp mesh: " + filePath);
			File file = new File(filePath);
			String fileDir = file.toURI().toURL().getFile().replace(specificFile, "");
			if ( !file.exists() ) {
				URL url = IOUtil.ioResourceGetURL(filePath);
				fileDir = url.getFile().replace(specificFile, "");
				file = new File( fileDir + specificFile );
			}
	
			String fileWithoutExtension = specificFile;
			int pos1 = fileWithoutExtension.lastIndexOf(".");
			if (pos1 != -1) {
				fileWithoutExtension = fileWithoutExtension.substring(0, pos1);
			}
	
			// Get scene
			int flags = aiProcess_JoinIdenticalVertices | aiProcess_Triangulate | Assimp.aiProcess_GenSmoothNormals;
			if ( extraFlags > 0 )
				flags = flags | extraFlags;
			AIScene scene = Assimp.aiImportFile(file.getAbsolutePath(), flags);
			if ( scene == null || scene.mNumMeshes() <= 0 )
				return null;
			
			// Get data
			ArrayList<AIMesh> meshes = assimpGetMeshes(scene.mMeshes());
			ArrayList<AIMaterial> materials = assimpGetMaterials(scene.mMaterials());
			ArrayList<AIAnimation> animations = assimpGetAnimations(scene.mAnimations());

			// Create animation data folder
			AnimationData aData = null;
			Instance boneData = null;
			if ( animations.size() > 0 ) {
				aData = new AnimationData();
				aData.forceSetParent(prefab);
				boneData = new Bones();
			}
			
			for ( int i = 0; i < meshes.size(); i++ ) {
				AIMesh mesh = meshes.get(i);
	
				// Create temp mesh
				BufferedMesh bm = new BufferedMesh( mesh.mNumFaces() * 3 );
				int vertCounter = 0;
				
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
						Vertex output = new Vertex( vertex.x()*scale, vertex.y()*scale, vertex.z()*scale, normalVector.x, normalVector.y, normalVector.z, textureCoords.x, textureCoords.y, 1, 1, 1, 1 );
						bm.setVertex(vertCounter, output);
						vertCounter++;
					}
				}
	
				// Load material
				int materialIndex = mesh.mMaterialIndex();
				Material tm = (materialIndex >= 0)?getMaterialFromAssimp( fileDir, materials.get(materialIndex) ):new Material();
				tm.setName(fileWithoutExtension+"_"+i);
				tm.forceSetParent(tm.getPreferredParent());
	
				// Load mesh
				Mesh mm = new Mesh();
				mm.setName(fileWithoutExtension+"_"+i);
				mm.setMesh(bm);
				mm.forceSetParent(mm.getPreferredParent());
	
				// Check for bones
				if ( aData != null ) {
					aData.processBones(mm, boneData, mesh.mBones());
				}
				
				prefab.addModel(mm, tm);
			}

			// Add animations
			if ( aData != null ) {
				boneData.forceSetParent(aData);
				aData.processAnimations(animations);
				aData.processBoneTree(scene.mRootNode(), null);
			}
			System.out.println("Loaded");
			
			prefab.setName(fileWithoutExtension);
			prefab.forceSetParent(prefab.getPreferredParent());
		} catch(Exception e ) {
			e.printStackTrace();
		}

		return prefab;
	}

	private static HashMap<AIMaterial, Material> materialLookup = new HashMap<AIMaterial, Material>();
	
	private static Material getMaterialFromAssimp( String baseDir, AIMaterial material ) {
		if ( materialLookup.containsKey(material) ) {
			Material ret = (Material) materialLookup.get(material).clone();
			ret.forceSetParent(materialLookup.get(material).getParent());
			return ret;
		}
		
		// Load textures
		String diffuse  = assimpGetTextureFile( material, Assimp.aiTextureType_DIFFUSE );
		String normal   = assimpGetTextureFile( material, Assimp.aiTextureType_NORMALS );
		String specular = assimpGetTextureFile( material, Assimp.aiTextureType_SPECULAR );
		String glossy   = assimpGetTextureFile( material, Assimp.aiTextureType_SHININESS );
		if ( diffuse != null )
			diffuse = baseDir+diffuse;
		if ( normal != null )
			normal = baseDir+normal;
		if ( specular != null )
			specular = baseDir+specular;
		if ( glossy != null )
			glossy = baseDir+glossy;

		// Create base material
		Material tm = new Material();
		{
			if ( diffuse != null ) {
				Texture t1 = new Texture();
				t1.forceSetName(diffuse.replace(baseDir, ""));
				t1.setSRGB(true);
				t1.setFilePath(diffuse);
				t1.forceSetParent(t1.getPreferredParent());
				tm.setDiffuseMap(t1);
			}
			
			if ( normal != null ) {
				Texture t2 = new Texture();
				t2.forceSetName(normal.replace(baseDir, ""));
				t2.setFilePath(normal);
				t2.forceSetParent(t2.getPreferredParent());
				tm.setNormalMap(t2);
			}
			
			if ( specular != null ) {
				Texture t3 = new Texture();
				t3.forceSetName(specular.replace(baseDir, ""));
				t3.setFilePath(specular);
				t3.forceSetParent(t3.getPreferredParent());
				tm.setMetalMap(t3);
			}
			
			if ( glossy != null ) {
				Texture t4 = new Texture();
				t4.forceSetName(glossy.replace(baseDir, ""));
				t4.setFilePath(glossy);
				t4.forceSetParent(t4.getPreferredParent());
				tm.setRoughMap(t4);
			}
		}
		
		// Load other textures
		System.out.println("Found material: " + material);
		PointerBuffer properties = material.mProperties();
		for (int j = 0; j < material.mNumProperties(); j++) {
			org.lwjgl.assimp.AIMaterialProperty prop = AIMaterialProperty.create(properties.get(j));
			String propertyKey = prop.mKey().dataString();
			System.out.println("  -" + prop.mKey().dataString());
			if ( propertyKey.contains("mat.name") ) {
				String name = byteBufferAsString(prop.mData());
				tm.setName(name);
			}
			if ( propertyKey.contains("clr.diffuse") ) {
				AIColor4D mDiffuseColor = AIColor4D.create();
				Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0, mDiffuseColor);
				tm.setColor( Color3.newInstance((int)(mDiffuseColor.r()*255f), (int)(mDiffuseColor.g()*255f), (int)(mDiffuseColor.b()*255f)));
			}
			if ( propertyKey.contains("mat.shininess") ) {
				float shine = prop.mData().getFloat();
				//tm.setMetalness(shine);
				//tm.setSpecularity(shine / 256f);
				//tm.setGlossiness(shine);
			}
		}
		
		materialLookup.put(material, tm);

		return tm;
	}

	private static String byteBufferAsString(ByteBuffer dat) {
		String ret = "";
		for (int k = 0; k < dat.capacity(); k++) {
			ret += (char)dat.get(k);
		}
		return ret.trim();
	}

	private static String assimpGetTextureFile( AIMaterial material, int textureType ) {
		if ( Assimp.aiGetMaterialTextureCount( material, textureType ) <= 0 )
			return null;

		AIString path = AIString.create();
		int texInd = Assimp.aiGetMaterialTexture(material, textureType, 0, path, new int[]{0}, new int[]{0}, new float[]{0}, new int[]{0}, new int[]{0}, new int[]{0});

		String filePath = path.dataString();
		String fileName = filePath.substring( filePath.lastIndexOf('/')+1, filePath.length() );
		System.out.println("Original texture File: " + filePath);
		return fileName;
	}

	private static ArrayList<AIMesh> assimpGetMeshes(PointerBuffer mMeshes) {
		ArrayList<AIMesh> meshes = new ArrayList<AIMesh>();
		
		if ( mMeshes == null )
			return meshes;
		
		for ( int i = 0; i < mMeshes.remaining(); i++ ) {
			meshes.add( AIMesh.create(mMeshes.get(i)) );
		}
		return meshes;
	}

	private static ArrayList<AIAnimation> assimpGetAnimations(PointerBuffer mAnims) {
		ArrayList<AIAnimation> array = new ArrayList<AIAnimation>();

		if ( mAnims == null )
			return array;
		
		for ( int i = 0; i < mAnims.remaining(); i++ ) {
			array.add( AIAnimation.create(mAnims.get(i)) );
		}
		return array;
	}

	private static ArrayList<AIMaterial> assimpGetMaterials(PointerBuffer mMeshes) {
		ArrayList<AIMaterial> meshes = new ArrayList<AIMaterial>();
		for ( int i = 0; i < mMeshes.remaining(); i++ ) {
			meshes.add( AIMaterial.create(mMeshes.get(i)) );
		}
		return meshes;
	}

	public AssetFolder materials() {
		return (AssetFolder) this.findFirstChild("Materials");
	}

	public AssetFolder meshes() {
		return (AssetFolder) this.findFirstChild("Meshes");
	}

	public AssetFolder textures() {
		return (AssetFolder) this.findFirstChild("Textures");
	}
	

	public AssetFolder prefabs() {
		return (AssetFolder) this.findFirstChild("Prefabs");
	}

	public Mesh newMesh() {
		Mesh mesh = new Mesh();
		mesh.setParent(meshes());
		return mesh;
	}
	
	public Texture newTexture() {
		Texture texture = new Texture();
		texture.setParent(textures());
		return texture;
	}
	
	public Material newMaterial() {
		Material material = new Material();
		material.setParent(materials());
		return material;
	}
	
	public Prefab newPrefab() {
		Prefab prefab = new Prefab();
		prefab.setParent(prefabs());
		return prefab;
	}
}
