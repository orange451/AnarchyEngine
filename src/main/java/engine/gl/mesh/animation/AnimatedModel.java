package engine.gl.mesh.animation;

import static org.lwjgl.opengl.GL30.glBindFragDataLocation;

import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

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
import engine.lua.type.object.insts.AnimationKeyframe;
import engine.lua.type.object.insts.AnimationKeyframeSequence;
import engine.lua.type.object.insts.Bone;
import engine.lua.type.object.insts.BoneTree;
import engine.lua.type.object.insts.BoneTreeNode;
import engine.lua.type.object.insts.BoneWeight;
import engine.lua.type.object.insts.Bones;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Prefab;

public class AnimatedModel {
	protected static final int MAX_BONES = 64;

	protected List<MaterialGL> materials = new ArrayList<>();
	protected List<AnimatedModelSubMesh> meshes = new ArrayList<>();
	protected final FloatBuffer boneBuffer = BufferUtils.createFloatBuffer(MAX_BONES * 16);
	protected AnimationController controller;
	private Matrix4f tempMat = new Matrix4f();
	
	private SkinningShader shader;
	
	public AnimatedModel(AnimationController controller) {
		this.controller = controller;
		this.rebuild();
		
		try {
			InternalRenderThread.runLater(()->{
				this.shader = new SkinningShader();
			});
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void rebuild() {
		meshes.clear();
		materials.clear();

		GameObject linked = controller.getLinkedInstance();
		Prefab prefab = linked.getPrefab();

		// Get animation data
		Instance aData = prefab.findFirstChildOfClass("AnimationData");
		if (aData == null)
			return;

		// Get bone folder
		Bones bones = (Bones) aData.findFirstChildOfClass("Bones");
		if (bones == null)
			return;
		
		// Compute bone indices
		HashMap<Bone, Integer> boneIndices = computeBoneIndices( new HashMap<>(), bones, aData.findFirstChildOfClass(BoneTree.class.getSimpleName()) );
		
		// Temporary data to store bone data
		HashMap<BufferedMesh, HashMap<Integer, List<Float>>> tempData1 = new HashMap<>();
		HashMap<BufferedMesh, HashMap<Integer, List<Integer>>> tempData2 = new HashMap<>();

		// Get all bone data
		List<Instance> children = bones.getChildrenOfClass("Bone");
		for (int i = 0; i < children.size(); i++) {

			// We can safely cast to bone -> Children list is guaranteed bones
			engine.lua.type.object.insts.Bone bone = (engine.lua.type.object.insts.Bone) children.get(i);

			Mesh mesh = bone.getMesh();
			BufferedMesh bufferedMesh = mesh.getMesh();
			if (!tempData1.containsKey(bufferedMesh)) {
				tempData1.put(bufferedMesh, new HashMap<>());
				tempData2.put(bufferedMesh, new HashMap<>());
			}

			HashMap<Integer, List<Float>> weightData = tempData1.get(bufferedMesh);
			HashMap<Integer, List<Integer>> indexData = tempData2.get(bufferedMesh);

			List<Instance> weights = bone.getChildren();
			for (int j = 0; j < weights.size(); j++) {
				Instance weight = weights.get(j);
				if (!(weight instanceof BoneWeight))
					continue;

				BoneWeight bWeight = (BoneWeight) weight;
				int ind = bWeight.getVertexId();
				float wei = bWeight.getWeight();

				// Store vertex ID Weights
				List<Float> weightsTemp = weightData.get(ind);
				if (weightsTemp == null) {
					weightsTemp = new ArrayList<Float>();
					weightData.put(ind, weightsTemp);
				}
				weightsTemp.add(wei);
				
				// Store Vertex ID Bone Indices
				List<Integer> bonesTemp = indexData.get(ind);
				if ( bonesTemp == null ) {
					bonesTemp = new ArrayList<Integer>();
					indexData.put(ind, bonesTemp);
				}
				
				Integer boneIndex = boneIndices.get(bone);
				if ( boneIndex == null )
					boneIndex = 0;
				bonesTemp.add(boneIndex);
			}
		}
		
		// Create sub meshes
		HashMap<BufferedMesh, AnimatedModelSubMesh> meshMap = new HashMap<>();
		for (Entry<BufferedMesh, HashMap<Integer, List<Float>>> entry : tempData1.entrySet()) {
			BufferedMesh key = entry.getKey();
			AnimatedModelSubMesh subMesh = new AnimatedModelSubMesh(key);
			meshMap.put(key, subMesh);
			meshes.add(subMesh);
		}

		// Put bone vertex weight data into sub meshes...
		for (Entry<BufferedMesh, HashMap<Integer, List<Float>>> entry : tempData1.entrySet()) {
			BufferedMesh key = entry.getKey();
			HashMap<Integer, List<Float>> value = entry.getValue();
			
			AnimatedModelSubMesh subMesh = meshMap.get(key);
			for (Entry<Integer, List<Float>> vertexData : value.entrySet()) {
				int index = vertexData.getKey();
				
				List<Float> weightVals = vertexData.getValue();
				subMesh.setBoneWeights(index, listToVector(weightVals));
			}
		}
		
		// Put bone index data into sub meshes
		for (Entry<BufferedMesh, HashMap<Integer, List<Integer>>> entry : tempData2.entrySet()) {
			BufferedMesh key = entry.getKey();
			HashMap<Integer, List<Integer>> value = entry.getValue();
			
			AnimatedModelSubMesh subMesh = meshMap.get(key);
			for (Entry<Integer, List<Integer>> vertexData : value.entrySet()) {
				int index = vertexData.getKey();
				
				List<Integer> boneInd = vertexData.getValue();
				subMesh.setBoneIndices(index, listToVector(boneInd));
			}
		}
	}
	
	/**
	 * Convert a n lengthed (max 4) list into a Vector4.
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
	private HashMap<Bone, Integer> computeBoneIndices(HashMap<Bone,Integer> hashMap, Instance bones, Instance root) {
		if ( root instanceof BoneTreeNode ) {
			Instance bone = bones.findFirstChild(root.getName());
			if ( bone != null && bone instanceof Bone ) {
				hashMap.put((Bone) bone, hashMap.size());
			}
		}
		
		List<Instance> children = root.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Instance newRoot = children.get(i);
			computeBoneIndices( hashMap, bones, newRoot );
		}
		
		return hashMap;
	}
	
	/**
	 * Updates the current bone buffer from the animation controllers bone data.
	 */
	public void updateBones() {
		HashMap<Bone, Matrix4> bones = controller.getBoneAbsolutePositions();
		if (bones.size() == 0)
			return;

		// Store bones to buffer
		for (Entry<Bone, Matrix4> entry : bones.entrySet()) {
			Bone bone = entry.getKey();
			Matrix4 absoluteMatrix = entry.getValue();
			Matrix4 bindMatrix = bone.getOffsetMatrix();
			
			absoluteMatrix.getInternal().mul(bindMatrix.getInternal(), tempMat);
			tempMat.get(boneBuffer);
		}
		boneBuffer.flip();
	}
	
	public void render(Matrix4f worldMatrix) {
		if ( shader == null )
			return;
		
		BaseShader shader = Pipeline.pipeline_get().shader_get();
		Pipeline.pipeline_get().shader_set(shader);
		
		// Send bones to GPU
		int boneLocation = shader.shader_get_uniform( "boneMat");
		if (boneLocation != -1) {
			GL20.glUniformMatrix4fv(boneLocation, false, boneBuffer);
		}

		// Loop through each material and render
		for (int i = 0; i < meshes.size(); i++) {
			Resources.MATERIAL_BLANK.bind(shader);
			meshes.get(i).render( shader, worldMatrix );
		}
		
		Pipeline.pipeline_get().shader_set(shader);
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
