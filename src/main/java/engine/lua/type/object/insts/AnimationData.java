package engine.lua.type.object.insts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AINodeAnim;
import org.lwjgl.assimp.AIQuatKey;
import org.lwjgl.assimp.AIVectorKey;
import org.lwjgl.assimp.AIVertexWeight;

import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class AnimationData extends Instance implements TreeViewable {

	public AnimationData() {
		super("AnimationData");
		this.setInstanceable(false);

		this.getField(LuaValue.valueOf("Archivable")).setLocked(true);
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
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_animation_data;
	}
	
	private Matrix4f fromAssimpMatrix(AIMatrix4x4 mOff) {
		return new Matrix4f(
				mOff.a1(), mOff.a2(), mOff.a3(), mOff.a4(),
				mOff.b1(), mOff.b2(), mOff.b3(), mOff.b4(),
				mOff.c1(), mOff.c2(), mOff.c3(), mOff.c4(),
				mOff.d1(), mOff.d2(), mOff.d3(), mOff.d4()
			).transpose();
	}

	public void processBones(Mesh mesh, HashMap<Integer, List<Integer>> indexToVertexIndex, Instance boneData, PointerBuffer mBones) {
		if ( mBones == null )
			return;

		for (int a = 0; a < mBones.remaining(); a++) {
			AIBone bone = AIBone.create(mBones.get(a));
			Matrix4f offsetMat = fromAssimpMatrix(bone.mOffsetMatrix());

			Bone b = new Bone();
			b.rawset("Mesh", mesh);
			b.rawset("OffsetMatrix", new Matrix4(offsetMat));
			b.forceSetName(bone.mName().dataString());

			for (int i = 0; i < bone.mNumWeights(); i++) {
				AIVertexWeight weight = bone.mWeights().get(i);
				
				int vertexIndex = weight.mVertexId();
				float vertexWeight = weight.mWeight();
				List<Integer> vertices = indexToVertexIndex.get(vertexIndex);
				
				for (int j = 0; j < vertices.size(); j++) {
					BoneWeight w = new BoneWeight();
					w.forceset("VertexId", LuaValue.valueOf(vertices.get(j)));
					w.forceset("Weight", LuaValue.valueOf(vertexWeight));
					w.forceSetParent(b);
				}
			}
			
			b.forceSetParent(boneData);
		}
	}
	
	private Instance boneTree;
	public void processBoneTree(AINode node, Instance parent) {
		// Create initial bone tree folder
		boolean c = false;
		if ( this.boneTree == null ) {
			this.boneTree = new BoneTree();
			c = true;
			
			Matrix4 temp = new Matrix4(fromAssimpMatrix(node.mTransformation()).invert());
			this.findFirstChildOfClass("Bones").rawset(Bones.C_ROOTINVERSE, temp);
		}
		
		if ( parent == null )
			parent = boneTree;
		
		// Create node object for this node
		BoneTreeNode t = new BoneTreeNode();
		t.forceSetName(node.mName().dataString());
		
		// Create children node objects
		for (int i = 0; i < node.mNumChildren(); i++) {
			AINode child = AINode.create(node.mChildren().get(i));
			processBoneTree( child, t );
		}
		
		// Put node object in parent
		t.forceSetParent(parent);
		
		if ( c )
			this.boneTree.forceSetParent(this);
	}

	public void processAnimations(ArrayList<AIAnimation> animations) {
		
		Instance anim = this.findFirstChild("Animations");
		if ( anim == null )
			anim = new Animations();

		for (int i = 0; i < animations.size(); i++) {
			AIAnimation aiAnimation = animations.get(i);

			Animation animObject = new Animation();
			String name = aiAnimation.mName().dataString();
			if ( name == null || name.length() == 0 )
				name = "Animation";
			animObject.forceSetName(name);
			
			TreeMap<Double,HashMap<Bone,TempKeyframe>> temp = new TreeMap<Double,HashMap<Bone,TempKeyframe>>();
			
			int nodes = aiAnimation.mNumChannels();
			for (int j = 0; j < nodes; j++) {
				AINodeAnim nodeData = AINodeAnim.create(aiAnimation.mChannels().get(j));

				Instance bones = this.findFirstChildOfClass("Bones");
				if ( bones == null )
					continue;

				List<Instance> boneList = bones.getChildrenWithName(nodeData.mNodeName().dataString());
				for (int a = 0; a < boneList.size(); a++) {
					Instance bone = boneList.get(a);
					if ( bone == null || !(bone instanceof Bone))
						continue;
	
					// Add in positions
					for (int k = 0; k < nodeData.mNumPositionKeys(); k++) {
						AIVectorKey key = nodeData.mPositionKeys().get(k);
						Double time = key.mTime();
	
						if ( !temp.containsKey(time) ) {
							temp.put(time, new HashMap<Bone,TempKeyframe>());
						}
	
						HashMap<Bone, TempKeyframe> keyframes = temp.get(time);
						if ( !keyframes.containsKey(bone) ) {
							keyframes.put((Bone) bone, new TempKeyframe((Bone) bone));
						}
						
						TempKeyframe keyframeModifier = keyframes.get(bone);
						keyframeModifier.position = key;
					}
	
					// Add in rotations
					for (int k = 0; k < nodeData.mNumRotationKeys(); k++) {
						AIQuatKey key = nodeData.mRotationKeys().get(k);
						Double time = key.mTime();
	
						if ( !temp.containsKey(time) ) {
							temp.put(time, new HashMap<Bone,TempKeyframe>());
						}
	
						HashMap<Bone, TempKeyframe> keyframes = temp.get(time);
						if ( !keyframes.containsKey(bone) ) {
							keyframes.put((Bone) bone, new TempKeyframe((Bone) bone));
						}
						
						TempKeyframe keyframeModifier = keyframes.get(bone);
						keyframeModifier.rotation = key;
					}
				}
			}
			
			// Translate list of keyframes into proper objects
			for (Map.Entry<Double,HashMap<Bone,TempKeyframe>> entry : temp.entrySet()) {
				Double time = entry.getKey();
				AnimationKeyframeSequence seq = new AnimationKeyframeSequence();
				
				seq.forceset("Time", LuaValue.valueOf(time.toString()));
				seq.forceSetName(time.toString());
				
				HashMap<Bone, TempKeyframe> keyframes = entry.getValue();
				for (Map.Entry<Bone,TempKeyframe> entryK : keyframes.entrySet()) {
					
					// Keyframe data
					TempKeyframe keyFrame = entryK.getValue();
					if ( keyFrame == null )
						continue;
					
					AIVectorKey keyFramePosition = keyFrame.position;
					AIQuatKey keyFrameRotation = keyFrame.rotation;
					
					// Compute translation matrix
					Matrix4f translation = new Matrix4f();
					if ( keyFramePosition != null ) {
						translation.translate(
							keyFrame.position.mValue().x(),
							keyFrame.position.mValue().y(),
							keyFrame.position.mValue().z()
						);
					}
					
					// Overall node transform is translation for now...
					Matrix4f nodeTransform = translation;
					
					// Compute rotation matrix
					if ( keyFrameRotation != null ) {
						Quaternionf rotation = new Quaternionf(
							keyFrame.rotation.mValue().x(),
							keyFrame.rotation.mValue().y(),
							keyFrame.rotation.mValue().z(),
							keyFrame.rotation.mValue().w()
						);
						
						// relative = offset * rotation
						nodeTransform = translation.rotate(rotation);
					}
					
					// Create keyframe
					AnimationKeyframe keyframe = new AnimationKeyframe();
					keyframe.forceset("Bone", entryK.getKey());
					keyframe.forceset("Matrix", new Matrix4(nodeTransform));
					keyframe.forceSetName(entryK.getKey().getName());
					keyframe.forceSetParent(seq);
				}
				
				seq.forceSetParent(animObject);
			}

			animObject.forceSetParent(anim);
		}

		anim.forceSetParent(this);
	}
	
	static class TempKeyframe {
		Bone bone;
		AIQuatKey rotation;
		AIVectorKey position;
		
		public TempKeyframe(Bone bone) {
			this.bone = bone;
		}
	}
}
