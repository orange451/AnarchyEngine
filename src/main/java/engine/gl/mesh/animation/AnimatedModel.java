/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.mesh.animation;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.luaj.vm2.LuaValue;
import org.lwjgl.system.MemoryUtil;

import engine.gl.mesh.BufferedMesh;
import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.animation.AnimationController;
import engine.lua.type.object.insts.animation.Bone;
import engine.lua.type.object.insts.animation.BoneTreeNode;
import engine.lua.type.object.insts.animation.BoneWeight;
import engine.lua.type.object.insts.animation.Bones;

public class AnimatedModel {
	protected static final int MAX_BONES = 64;
	protected static final int VALUES_PER_MATRIX = 16;

	private final FloatBuffer boneBuffer = MemoryUtil.memAllocFloat(MAX_BONES * VALUES_PER_MATRIX);
	private final FloatBuffer lastBoneBuffer = MemoryUtil.memAllocFloat(MAX_BONES * VALUES_PER_MATRIX);
	private FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(VALUES_PER_MATRIX);
	private HashMap<String, Integer> boneIndices;
	private HashMap<Integer, String> indexToBoneMap;
	private Bones bonesFolder;
	
	protected HashMap<AnimatedModelSubMesh, Model> meshToModelMap = new HashMap<>();
	protected List<AnimatedModelSubMesh> meshes = new ArrayList<>();
	protected AnimationController controller;
	
	private Matrix4f tempMat;
	
	public AnimatedModel(AnimationController controller) {
		this.controller = controller;
		this.tempMat = new Matrix4f();
	}
	
	private static final LuaValue C_BONETREE = LuaValue.valueOf("BoneTree");
	
	private void rebuild() {
		meshes.clear();
		meshToModelMap.clear();

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
			engine.lua.type.object.insts.animation.Bone bone = (engine.lua.type.object.insts.animation.Bone) children.get(i);

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
				if ( model.getMeshInternal().equals(key) ) {
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

		MemoryUtil.memCopy(boneBuffer, lastBoneBuffer);

		// Store bones to buffer
		boneBuffer.rewind();
		for (int i = 0; i < boneIndices.size(); i++) {
			String boneName = indexToBoneMap.get(i);
			Bone bone = (Bone) bonesFolder.findFirstChild(boneName);
			if ( bone == null )
				continue;
			
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
	
	// TODO: In fact, only rebuilds the buffers
	public void renderV2() {
		if (meshes.size() == 0)
			rebuild();
		updateBones();
	}

	public FloatBuffer getBoneBuffer() {
		return boneBuffer;
	}

	public FloatBuffer getPreviousBoneBuffer() {
		return lastBoneBuffer;
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
}
