package engine.gl.mesh.animation;

import static org.lwjgl.opengl.GL30.glBindFragDataLocation;

import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.luaj.vm2.LuaValue;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import engine.InternalRenderThread;
import engine.gl.MaterialGL;
import engine.gl.Pipeline;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.gl.renderer.GBuffer;
import engine.gl.shader.BaseShader;
import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.AnimationController;
import engine.lua.type.object.insts.Bone;
import engine.lua.type.object.insts.BoneTree;
import engine.lua.type.object.insts.BoneTreeNode;
import engine.lua.type.object.insts.BoneWeight;
import engine.lua.type.object.insts.Bones;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;

public class AnimatedModel {
	protected static final int MAX_BONES = 128;
	protected static final int VALUES_PER_MATRIX = 16;

	private final FloatBuffer boneBuffer = MemoryUtil.memAllocFloat(MAX_BONES * VALUES_PER_MATRIX);
	private FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(VALUES_PER_MATRIX);
	private HashMap<String, Integer> boneIndices;
	private HashMap<Integer, String> indexToBoneMap;
	private Bones bonesFolder;
	private Prefab prefab;
	
	protected HashMap<AnimatedModelSubMesh, Model> meshToModelMap = new HashMap<>();
	protected List<AnimatedModelSubMesh> meshes = new ArrayList<>();
	protected AnimationController controller;
	
	private Matrix4f tempMat;
	private SkinningShader shader;
	
	public AnimatedModel(AnimationController controller) {
		this.controller = controller;
		this.tempMat = new Matrix4f();
		
		InternalRenderThread.runLater(()->{
			this.shader = new SkinningShader();
		});
	}
	
	private static final LuaValue C_BONETREE = LuaValue.valueOf("BoneTree");
	
	private void rebuild() {
		meshes.clear();
		meshToModelMap.clear();

		GameObject linked = controller.getLinkedInstance();
		Prefab prefab = linked.getPrefab();
		this.prefab = prefab;

		// Get animation data
		Instance aData = prefab.findFirstChildOfClass("AnimationData");
		if (aData == null)
			return;

		// Get bone folder
		Bones bones = (Bones) aData.findFirstChildOfClass("Bones");
		if (bones == null)
			return;
		
		bonesFolder = bones;
		
		// Compute bone indices
		boneIndices = new HashMap<>();
		indexToBoneMap = new HashMap<>();
		computeBoneIndices( bones, aData.findFirstChildOfClass(C_BONETREE) );
		
		// Temporary data to store bone data
		HashMap<BufferedMesh, HashMap<Integer, BoneData>> tempData1 = new HashMap<>();

		// Get all bone data
		List<Instance> children = bones.getChildrenOfClass("Bone");
		for (int i = 0; i < children.size(); i++) {

			// We can safely cast to bone -> Children list is guaranteed bones
			engine.lua.type.object.insts.Bone bone = (engine.lua.type.object.insts.Bone) children.get(i);

			Mesh mesh = bone.getMesh();
			BufferedMesh bufferedMesh = mesh.getMesh();
			if (!tempData1.containsKey(bufferedMesh)) {
				tempData1.put(bufferedMesh, new HashMap<>());
			}

			HashMap<Integer, BoneData> weightData = tempData1.get(bufferedMesh);

			// Write bone weights and indices
			List<Instance> weights = bone.getChildren();
			for (int j = 0; j < weights.size(); j++) {
				Instance weight = weights.get(j);
				if (!(weight instanceof BoneWeight))
					continue;

				BoneWeight bWeight = (BoneWeight) weight;
				int ind = bWeight.getVertexId();
				float wei = bWeight.getWeight();

				// Store vertex ID Weights
				BoneData weightsTemp = weightData.get(ind);
				if (weightsTemp == null) {
					weightsTemp = new BoneData();
					weightData.put(ind, weightsTemp);
				}
				
				// Find the bone this weight is attached to
				Integer boneIndex = boneIndices.get(bone.getName());
				if ( boneIndex == null )
					boneIndex = 0;
				
				weightsTemp.weights.add(wei);
				weightsTemp.indices.add(boneIndex);
			}
		}

		// Put bone vertex weight data into sub meshes...
		for (Entry<BufferedMesh, HashMap<Integer, BoneData>> entry : tempData1.entrySet()) {
			BufferedMesh key = entry.getKey();
			HashMap<Integer, BoneData> value = entry.getValue();

			AnimatedModelSubMesh subMesh = new AnimatedModelSubMesh(key);
			for (Entry<Integer, BoneData> vertexData : value.entrySet()) {
				int index = vertexData.getKey();
				BoneData boneData = vertexData.getValue();
				
				subMesh.setBoneWeights(index, listToVector(boneData.weights));
				subMesh.setBoneIndices(index, listToVector(boneData.indices));
			}
			meshes.add(subMesh);
			
			// Grab the model (used for material data)
			List<Model> models = prefab.getModels();
			for (int i = 0; i < models.size(); i++) {
				Model model = models.get(i);
				if ( model.getMesh().equals(key) ) {
					meshToModelMap.put(subMesh, model);
				}
			}
		}
	}
	
	/**
	 * Convert a n length'd (max 4) list into a Vector4.
	 * @param list
	 * @return
	 */
	private Vector4f listToVector(List<?> list) {
		Vector4f ret = new Vector4f(0);
		
		if ( list.size() > 0 )
			ret.x = tonumber(list.get(0));
		if ( list.size() > 1 )
			ret.y = tonumber(list.get(1));
		if ( list.size() > 2 )
			ret.z = tonumber(list.get(2));
		if ( list.size() > 3 )
			ret.w = tonumber(list.get(3));
		
		return ret;
	}
	
	/**
	 * Convert generic object to number
	 * @param object
	 * @return
	 */
	private float tonumber(Object object) {
		if ( object instanceof Float ) {
			return ((Float)object).floatValue();
		}
		
		if ( object instanceof Integer ) {
			return ((Integer)object).intValue();
		}
		
		return Float.parseFloat(object.toString());
	}

	/**
	 * Recursively compute the bone indices.
	 * @param hashMap
	 * @param bones
	 * @param root
	 * @return
	 */
	private void computeBoneIndices(Instance bones, Instance root) {
		if ( root instanceof BoneTreeNode ) {
			Instance bone = bones.findFirstChild(root.getName());
			if ( bone != null && bone instanceof Bone ) {
				int index = boneIndices.size();
				boneIndices.put(bone.getName(), index);
				indexToBoneMap.put(index, bone.getName());
			}
		}
		
		List<Instance> children = root.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Instance newRoot = children.get(i);
			computeBoneIndices( bones, newRoot );
		}
	}
	
	/**
	 * Updates the current bone buffer from the animation controllers bone data.
	 */
	private void updateBones() {
		HashMap<String, Matrix4> bones = controller.getBoneAbsolutePositions();
		if (bones.size() == 0)
			return;
		
		if ( boneIndices.size() == 0 )
			return;

		// Store bones to buffer
		boneBuffer.rewind();
		for (int i = 0; i < boneIndices.size(); i++) {
			String boneName = indexToBoneMap.get(i);
			Bone bone = (Bone) bonesFolder.findFirstChild(boneName);
			
			Matrix4 absoluteMatrix = bones.get(boneName);
			Matrix4 bindMatrix = bone.getOffsetMatrix();
			
			// Multiple absolute matrix by bind matrix
			if ( absoluteMatrix != null && bindMatrix != null ) {
				absoluteMatrix.getInternal().mul(bindMatrix.getInternal(), tempMat);
			}
			
			// Store matrix to buffer
			matrixBuffer.rewind();
			tempMat.get(matrixBuffer);
			
			// Put matrixbuffer in bone buffer
			boneBuffer.put(matrixBuffer);
		}
		boneBuffer.flip();
	}
	
	private Matrix4f renderMat = new Matrix4f();
	public void render(Matrix4f worldMatrix) {
		if ( shader == null )
			return;
		
		// Compute absolute positions of bones
		if ( meshes.size() == 0 )
			rebuild();
		updateBones();
		
		// Apply skinning shader
		BaseShader oldShader = Pipeline.pipeline_get().shader_get();
		Pipeline.pipeline_get().shader_set(shader);
		
		// Send bones to GPU
		int boneLocation = this.shader.shader_get_uniform("boneMat");
		if (boneLocation != -1) {
			GL20.glUniformMatrix4fv(boneLocation, false, boneBuffer);
		}

		// Set material/world matrix
		Resources.MATERIAL_BLANK.bind(this.shader);
		this.renderMat.set(worldMatrix);
		this.renderMat.scale(prefab.getScale());
		this.shader.setWorldMatrix(this.renderMat);
		
		// Loop through each mesh and render
		for (int i = 0; i < meshes.size(); i++) {
			AnimatedModelSubMesh mesh = this.meshes.get(i);
			
			// Bind mesh
			mesh.bind();
			
			// Bind material
			Model model = meshToModelMap.get(mesh);
			Material material = model.getMaterial();
			if ( material != null ) {
				MaterialGL GLMat = material.getMaterial();
				if ( GLMat != null ) {
					GLMat.bind(this.shader);
				}
			}
			
			// Draw mesh
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, mesh.size());
			mesh.unbind();
		}
		
		Pipeline.pipeline_get().shader_set(oldShader);
	}
	
	// TODO: In fact, only rebuilds the buffers
	public void renderV2() {
		if (meshes.size() == 0)
			rebuild();
		updateBones();
	}

	public FloatBuffer getBoneBuffer() {
		return boneBuffer;
	}

	public List<AnimatedModelSubMesh> getMeshes() {
		return meshes;
	}

	public HashMap<AnimatedModelSubMesh, Model> getMeshToModelMap() {
		return meshToModelMap;
	}
	
	static class BoneData {
		public List<Float> weights;
		public List<Integer> indices;
		
		public BoneData() {
			this.weights = new ArrayList<Float>();
			this.indices = new ArrayList<Integer>();
		}
	}
	
	static class SkinningShader extends BaseShader {
		public SkinningShader() {
			super(
				new URL[] {
						GBuffer.class.getResource("skinningDeferred.vert")
				},
				new URL[] {
						GBuffer.class.getResource("normalmap.frag"),
						GBuffer.class.getResource("reflect.frag"),
						GBuffer.class.getResource("fresnel.frag"),
						GBuffer.class.getResource("reflectivePBR.frag"),
						GBuffer.class.getResource("write.frag"),
						GBuffer.class.getResource("deferred.frag")
				}
			);
		}
		
		@Override
		public void create(int id) {
			glBindFragDataLocation(id, 0, "gBuffer0");
			glBindFragDataLocation(id, 1, "gBuffer1");
			glBindFragDataLocation(id, 2, "gBuffer2");
			glBindFragDataLocation(id, 3, "gBuffer3");
		}
	}
}
