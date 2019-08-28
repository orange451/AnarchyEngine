package engine.gl.mesh.animation;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import engine.gl.MaterialGL;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
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
	
	public AnimatedModel(AnimationController controller) {
		this.controller = controller;
		this.rebuild();
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
				bonesTemp.add(boneIndices.get(bone));
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
			ret.x = (float) list.get(0);
		if ( list.size() > 1 )
			ret.y = (float) list.get(1);
		if ( list.size() > 2 )
			ret.z = (float) list.get(2);
		if ( list.size() > 3 )
			ret.w = (float) list.get(3);
		
		return ret;
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
		
	}
}
