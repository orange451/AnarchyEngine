/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.services;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.lwjgl.system.MemoryStack;

import engine.Game;
import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.Vertex;
import engine.lua.type.data.Color3;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.AssetFolder;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.AudioSource;
import engine.lua.type.object.insts.Texture;
import engine.lua.type.object.insts.animation.AnimationData;
import engine.lua.type.object.insts.animation.Bones;
import engine.util.FileUtils;
import engine.util.IOUtil;
import engine.util.MeshUtils;
import ide.layout.windows.icons.Icons;

public class Assets extends Service implements TreeViewable {

	public static final LuaValue C_MATERIALS = LuaValue.valueOf("Materials");
	public static final LuaValue C_MESHES = LuaValue.valueOf("Meshes");
	public static final LuaValue C_TEXTURES = LuaValue.valueOf("Textures");
	public static final LuaValue C_PREFABS = LuaValue.valueOf("Prefabs");
	public static final LuaValue C_AUDIO = LuaValue.valueOf("Audio");
	
	public Assets() {
		super("Assets");
		
		this.getmetatable().set("ImportMesh", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue path) {
				Mesh m = new Mesh();
				m.set("FilePath", path.toString());
				m.forceSetParent(meshes());
				return m;
			}
		});
		
		this.getmetatable().set("ImportTexture", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg3) {
				return importTexture(arg3.toString(), textures());
			}
		});
		
		this.getmetatable().set("ImportPrefab", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue path) {
				return importPrefab(path.toString(), Game.assets().prefabs());
			}
		});
		
		this.getmetatable().set("NewMesh", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Mesh t = new Mesh();
				t.forceSetParent(meshes());
				return t;
			}
		});
		
		this.getmetatable().set("NewTexture", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Texture t = new Texture();
				t.forceSetParent(textures());
				return t;
			}
		});
		
		this.getmetatable().set("NewMaterial", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Material m = new Material();
				m.forceSetParent(materials());
				return m;
			}
		});
		
		this.getmetatable().set("NewPrefab", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Prefab p = new Prefab();
				p.forceSetParent(prefabs());
				return p;
			}
		});
	}

	@Override
	protected void onLuaCreate() {
		if ( this.findFirstChild(Assets.C_PREFABS) == null )
			Assets.newPackage(Assets.C_PREFABS, this);

		if ( this.findFirstChild(Assets.C_MESHES) == null )
			Assets.newPackage(Assets.C_MESHES, this);

		if ( this.findFirstChild(Assets.C_MATERIALS) == null )
			Assets.newPackage(Assets.C_MATERIALS, this);

		if ( this.findFirstChild(Assets.C_TEXTURES) == null )
			Assets.newPackage(Assets.C_TEXTURES, this);

		if ( this.findFirstChild(Assets.C_AUDIO) == null )
			Assets.newPackage(Assets.C_AUDIO, this);		
	}
	
	public AudioSource importSound(String filepath, Instance parent) {
		AudioSource t = new AudioSource();
		t.setFilePath(filepath);
		t.forceSetParent(parent);
		return t;
	}
	
	public Texture importTexture(String filepath, Instance parent) {
		Texture t = new Texture();
		t.setFilePath(filepath);
		t.forceSetParent(parent);
		return t;
	}
	
	/**
	 * Import a prefab via filepath.
	 * @param filePath
	 * @return
	 */
	public Prefab importPrefab(String filePath) {
		return importPrefab(filePath, null);
	}

	/**
	 * Import a prefab and parent it to the specified instance
	 * @param filePath
	 * @param instance
	 * @return
	 */
	public Prefab importPrefab(String filePath, Instance instance) {
		return importPrefab(filePath, 1, -1, instance);
	}

	public static Instance newPackage(String name, Instance parent) {
		return newPackage(LuaValue.valueOf(name), parent);
	}

	public static Instance newPackage(LuaValue name, Instance parent) {
		AssetFolder folder = new AssetFolder();
		folder.rawset("Name", name);
		folder.forceSetParent(parent);
		
		return folder;
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
	
	/**
	 * Get list of all textures loaded
	 * @return
	 */
	public List<Material> getMaterials() {
		List<Material> assets = new ArrayList<>();
		List<Instance> d = this.getDescendantsUnsafe();
		
		for (int i = 0; i < d.size(); i++) {
			Instance t = d.get(i);
			if ( t instanceof Material ) {
				assets.add((Material) t);
			}
		}
		
		return assets;
	}
	
	/**
	 * Get list of all textures loaded
	 * @return
	 */
	public List<AssetLoadable> getTextures() {
		List<AssetLoadable> assets = new ArrayList<AssetLoadable>();
		List<Instance> d = this.getDescendantsUnsafe();
		
		for (int i = 0; i < d.size(); i++) {
			Instance t = d.get(i);
			if ( t instanceof Texture ) {
				assets.add((AssetLoadable) t);
			}
		}
		
		return assets;
	}
	
	/**
	 * Get list of all meshes loaded
	 * @return
	 */
	public List<AssetLoadable> getMeshes() {
		List<AssetLoadable> assets = new ArrayList<AssetLoadable>();
		List<Instance> d = this.getDescendantsUnsafe();
		
		for (int i = 0; i < d.size(); i++) {
			Instance t = d.get(i);
			if ( t instanceof Mesh ) {
				assets.add((AssetLoadable) t);
			}
		}
		
		return assets;
	}
	
	/**
	 * Get list of all audio sources loaded
	 * @return
	 */
	public List<AssetLoadable> getAudio() {
		List<AssetLoadable> assets = new ArrayList<AssetLoadable>();
		List<Instance> d = this.getDescendantsUnsafe();
		
		for (int i = 0; i < d.size(); i++) {
			Instance t = d.get(i);
			if ( t instanceof AudioSource ) {
				assets.add((AssetLoadable) t);
			}
		}
		
		return assets;
	}
	
	//String fileName = Thread.currentThread().getContextClassLoader().getResource("model.obj").getFile();
	/**
	 * Import a static-model using assimp.
	 * @param filePath
	 * @return
	 */
	private static Prefab importPrefab(String filePath, float scale, int extraFlags, Instance parent) {
		String specificFile = FileUtils.getFileNameFromPath(filePath);
		Prefab prefab = (Prefab) Instance.instanceLua(Prefab.class.getSimpleName());
		
		Map<String, Texture> cachedTextures = new HashMap<>();
		
		try {
			System.out.println("Loading assimp mesh: " + filePath);
			
			// Get File
			File file = new File(filePath);
			String fileDir = file.toURI().toURL().getFile().replace(specificFile, "");
			if ( !file.exists() ) {
				URL url = IOUtil.ioResourceGetURL(filePath);
				fileDir = url.getFile().replace(specificFile, "");
				file = new File( fileDir + specificFile );
			}
	
			// Full patch without extension
			String fileWithoutExtension = FileUtils.getFileNameWithoutExtension(specificFile);
	
			// Get scene
			AIScene scene = MeshUtils.ImportAssimp(file.getAbsolutePath(), extraFlags);
			System.out.println(scene);
			if ( scene == null ) {
				System.out.println(Assimp.aiGetErrorString());
				return null;
			}
			
			// Get data
			ArrayList<AIMesh> meshes = assimpGetMeshes(scene.mMeshes());
			ArrayList<AIMaterial> materials = assimpGetMaterials(scene.mMaterials());
			ArrayList<AIAnimation> animations = assimpGetAnimations(scene.mAnimations());

			// Create animation data folder
			AnimationData aData = null;
			Instance boneData = null;
			if ( animations.size() > 0 ) {
				aData = new AnimationData();
				boneData = new Bones();
			}
			
			for ( int i = 0; i < meshes.size(); i++ ) {
				AIMesh mesh = meshes.get(i);
	
				// Create temp mesh
				BufferedMesh bm = new BufferedMesh( mesh.mNumFaces() * 3 );
				int vertCounter = 0;
				
				// Create mesh object (used to house the actual mesh)
				Mesh mm = new Mesh();
				mm.setName(fileWithoutExtension+"_"+i);
				
				// This is real hacky to get the actual vertex indices from the assimp indices... I don't like it. I should have planned ahead for this
				// But I didn't, and now im stuck. Oh well...
				HashMap<Integer, List<Integer>> indexToVertexIndex = new HashMap<>();
				
				// Get every face in mesh
				org.lwjgl.assimp.AIVector3D.Buffer vertices = mesh.mVertices();
				org.lwjgl.assimp.AIVector3D.Buffer normals = mesh.mNormals();
				org.lwjgl.assimp.AIVector3D.Buffer tangents = mesh.mTangents();
				org.lwjgl.assimp.AIFace.Buffer faces = mesh.mFaces();
				for (int j = 0; j < mesh.mNumFaces(); j++) {
					AIFace face = faces.get(j);
					IntBuffer indices = face.mIndices();
	
					// Loop through each index
					for (int k = 0; k < indices.capacity(); k++) {
						int index = indices.get(k);
						List<Integer> vertexMap = indexToVertexIndex.get(index);
						if ( vertexMap == null ) {
							vertexMap = new ArrayList<Integer>();
							indexToVertexIndex.put(index, vertexMap);
						}
						
						// Vert Data
						Vector2f textureCoords = new Vector2f();
						Vector3f normalVector = new Vector3f();
						Vector3f tangentVector = new Vector3f();
	
						// Get the vertex info for this index.
						AIVector3D vertex = vertices.get(index);
						if ( normals != null ) {
							AIVector3D normal = normals.get(index);
							normalVector.set(normal.x(),normal.y(),normal.z());
						}
						if ( tangents != null ) {
							AIVector3D tangent = tangents.get(index);
							tangentVector.set(tangent.x(), tangent.y(), tangent.z());
						}
						if ( mesh.mTextureCoords(0)!=null ) {
							AIVector3D tex = mesh.mTextureCoords(0).get(index);
							textureCoords.set(tex.x(), tex.y());
						}
	
						// Send vertex to output mesh
						Vertex output = new Vertex( vertex.x()*scale, vertex.y()*scale, vertex.z()*scale, normalVector.x, normalVector.y, normalVector.z, textureCoords.x, textureCoords.y, 1, 1, 1, 1 );
						output.setTangentXYZ(tangentVector.x, tangentVector.y, tangentVector.z);
						bm.setVertex(vertCounter, output);
						vertexMap.add(vertCounter);
						vertCounter++;
					}
				}
	
				// Load material
				int materialIndex = mesh.mMaterialIndex();
				Material tm = (materialIndex >= 0)?getMaterialFromAssimp( fileDir, materials.get(materialIndex), prefab, cachedTextures ):new Material();
				
				// Get asset folder material
				Instance materialAssetFolder = prefab.findFirstChild(tm.getPreferredParent());
				if ( materialAssetFolder == null )
					materialAssetFolder = newPackage(tm.getPreferredParent().toString(), prefab);
				tm.setName(fileWithoutExtension+"_"+i);
				tm.forceSetParent(materialAssetFolder);
				
				// Get asset folder mesh
				Instance meshAssetFolder = prefab.findFirstChild(mm.getPreferredParent());
				if ( meshAssetFolder == null )
					meshAssetFolder = newPackage(mm.getPreferredParent().toString(), prefab);
	
				// Load mesh
				mm.setMesh(bm);
				mm.forceSetParent(meshAssetFolder);
				
	
				// Check for bones
				if ( aData != null ) {
					aData.processBones(mm, indexToVertexIndex, boneData, mesh.mBones());
				}
				
				prefab.addModel(mm, tm);
			}

			// Add animations
			if ( aData != null ) {
				boneData.forceSetParent(aData);
				aData.processAnimations(animations);
				aData.processBoneTree(scene.mRootNode(), null);
				aData.forceSetParent(prefab);
			}
			System.out.println("Loaded");
			
			prefab.setName(fileWithoutExtension);
			if ( parent != null )
				prefab.forceSetParent(parent);
		} catch(Exception e ) {
			e.printStackTrace();
		}

		return prefab;
	}

	private static HashMap<AIMaterial, Material> materialLookup = new HashMap<AIMaterial, Material>();
	
	private static Material getMaterialFromAssimp( String baseDir, AIMaterial material, Prefab prefab, Map<String,Texture> cachedTextures ) {
		if ( materialLookup.containsKey(material) ) {
			Material ret = (Material) materialLookup.get(material).clone();
			ret.forceSetParent(materialLookup.get(material).getParent());
			return ret;
		}
		
		// Load textures
		String diffuse  = assimpGetTextureFile( material, Assimp.aiTextureType_DIFFUSE, baseDir );
		String normal   = assimpGetTextureFile( material, Assimp.aiTextureType_NORMALS, baseDir );
		String specular = assimpGetTextureFile( material, Assimp.aiTextureType_SPECULAR, baseDir );
		String glossy   = assimpGetTextureFile( material, Assimp.aiTextureType_SHININESS, baseDir );
		
		Texture TEMPTEXTURE = new Texture();
		Instance textureAssetFolder = prefab.findFirstChild(TEMPTEXTURE.getPreferredParent());
		if ( textureAssetFolder == null )
			textureAssetFolder = newPackage(TEMPTEXTURE.getPreferredParent().toString(), prefab);

		// Create base material
		Material tm = new Material();
		{
			if ( diffuse != null ) {
				if ( cachedTextures.containsKey(diffuse) ) {
					tm.setDiffuseMap(cachedTextures.get(diffuse));
				} else {
					Texture t1 = new Texture();
					t1.forceSetName(diffuse.replace(baseDir, ""));
					t1.setSRGB(true);
					t1.setFilePath(diffuse);
					t1.forceSetParent(textureAssetFolder);
					tm.setDiffuseMap(t1);
					cachedTextures.put(diffuse, t1);
				}
			}
			
			if ( normal != null ) {
				if ( cachedTextures.containsKey(normal) ) {
					tm.setNormalMap(cachedTextures.get(normal));
				} else {
					Texture t2 = new Texture();
					t2.forceSetName(normal.replace(baseDir, ""));
					t2.setFilePath(normal);
					t2.forceSetParent(textureAssetFolder);
					tm.setNormalMap(t2);
					cachedTextures.put(diffuse, t2);
				}
			}
			
			if ( specular != null ) {
				if ( cachedTextures.containsKey(specular) ) {
					tm.setRoughMap(cachedTextures.get(specular));
				} else {
					Texture t3 = new Texture();
					t3.forceSetName(specular.replace(baseDir, ""));
					t3.setFilePath(specular);
					t3.forceSetParent(textureAssetFolder);
					tm.setRoughMap(t3);
					cachedTextures.put(diffuse, t3);
				}
			}
			
			if ( glossy != null ) {
				if ( cachedTextures.containsKey(glossy) ) {
					tm.setMetalMap(cachedTextures.get(glossy));
				} else {
					Texture t4 = new Texture();
					t4.forceSetName(glossy.replace(baseDir, ""));
					t4.setFilePath(glossy);
					t4.forceSetParent(textureAssetFolder);
					tm.setMetalMap(t4);
					cachedTextures.put(diffuse, t4);
				}
			}
		}
		
		// Load other textures
		System.out.println("Found material: " + material);
		PointerBuffer properties = material.mProperties();
		for (int j = 0; j < material.mNumProperties(); j++) {
			AIMaterialProperty prop = AIMaterialProperty.create(properties.get(j));
			String propertyKey = prop.mKey().dataString();
			System.out.println("  -" + prop.mKey().dataString());
			if ( propertyKey.equals(Assimp.AI_MATKEY_NAME) ) {
				String name = byteBufferAsString(prop.mData());
				tm.setName(name);
			}
			
			if ( propertyKey.equals(Assimp.AI_MATKEY_COLOR_DIFFUSE) ) {
				try (MemoryStack stack = MemoryStack.stackPush()) {
					AIColor4D mDiffuseColor = AIColor4D.mallocStack(stack);
					Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0,
							mDiffuseColor);
					tm.setColor(Color3.newInstance((int) (mDiffuseColor.r() * 255f), (int) (mDiffuseColor.g() * 255f),
							(int) (mDiffuseColor.b() * 255f)));
				}
			}
			
			if ( propertyKey.equals(Assimp.AI_MATKEY_COLOR_SPECULAR) ) {
				try (MemoryStack stack = MemoryStack.stackPush()) {
					AIColor4D pbr = AIColor4D.mallocStack(stack);
					Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_COLOR_SPECULAR, Assimp.aiTextureType_NONE, 0,
							pbr);
					tm.setRoughness(pbr.r());
					tm.setMetalness(pbr.g());
				}
			}
			
			if ( propertyKey.equals(Assimp.AI_MATKEY_SHININESS) ) {
				float shine = prop.mData().getFloat();
				tm.setReflective(shine);
			}
			
			if ( propertyKey.equals(Assimp.AI_MATKEY_OPACITY)) {
				float opacity = prop.mData().getFloat();
				tm.setTransparency(1.0f-opacity);
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

	private static String assimpGetTextureFile( AIMaterial material, int textureType, String basePath ) {
		if ( Assimp.aiGetMaterialTextureCount( material, textureType ) <= 0 )
			return null;
		String fileName;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			AIString path = AIString.mallocStack(stack);
			Assimp.aiGetMaterialTexture(material, textureType, 0, path, new int[] { 0 }, new int[] { 0 },
					new float[] { 0 }, new int[] { 0 }, new int[] { 0 }, new int[] { 0 });
			String localBasePath = new String(basePath);

			String file = path.dataString();
			file = file.replace("\\", "/");
			file = file.replace("//", "");
			int count = file.split("\\.").length;
			if (count > 2) {
				count--;
				count /= 2;
				int lastSlash = localBasePath.lastIndexOf("/");
				if ( lastSlash > -1 ) {
					if (lastSlash == localBasePath.length() - 1)
						localBasePath = localBasePath.substring(0, lastSlash);
					
					for (int i = 0; i < count; i++)
						localBasePath = localBasePath.substring(0, lastSlash);
					file = file.substring(2);
				}
			} else {
				if (localBasePath.lastIndexOf("/") != localBasePath.length() - 1)
					localBasePath += "/";
			}

			fileName = localBasePath + file;
			System.out.println("Original texture File: " + file);
		}
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

	private static ArrayList<AIMaterial> assimpGetMaterials(PointerBuffer mMaterials) {
		ArrayList<AIMaterial> meshes = new ArrayList<AIMaterial>();
		
		if ( mMaterials != null ) {
			for ( int i = 0; i < mMaterials.remaining(); i++ ) {
				meshes.add( AIMaterial.create(mMaterials.get(i)) );
			}
		}
		
		return meshes;
	}

	public AssetFolder materials() {
		return (AssetFolder) this.findFirstChild(C_MATERIALS);
	}

	public AssetFolder meshes() {
		return (AssetFolder) this.findFirstChild(C_MESHES);
	}

	public AssetFolder textures() {
		return (AssetFolder) this.findFirstChild(C_TEXTURES);
	}

	public AssetFolder prefabs() {
		return (AssetFolder) this.findFirstChild(C_PREFABS);
	}

	public AssetFolder audio() {
		return (AssetFolder) this.findFirstChild(C_AUDIO);
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
