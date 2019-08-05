package engine.lua.type.object.insts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joml.Matrix4f;
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

		this.setLocked(true);
		this.setInstanceable(false);

		this.getField("Archivable").setLocked(true);
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

	public void processBones(Mesh mesh, Instance boneData, PointerBuffer mBones) {
		if ( mBones == null )
			return;

		for (int a = 0; a < mBones.remaining(); a++) {
			AIBone bone = AIBone.create(mBones.get(a));
			AIMatrix4x4 mOff = bone.mOffsetMatrix();
			Matrix4f offsetMat = new Matrix4f(
					mOff.a1(), mOff.a2(), mOff.a3(), mOff.a4(),
					mOff.b1(), mOff.b2(), mOff.b3(), mOff.b4(),
					mOff.c1(), mOff.c2(), mOff.c3(), mOff.c4(),
					mOff.d1(), mOff.d2(), mOff.d3(), mOff.d4()
				);
			offsetMat.transpose();

			Bone b = new Bone();
			b.rawset("Mesh", mesh);
			b.rawset("OffsetMatrix", new Matrix4(offsetMat));
			b.forceSetName(bone.mName().dataString());

			for (int i = 0; i < bone.mNumWeights(); i++) {
				AIVertexWeight weight = bone.mWeights().get(i);

				BoneWeight w = new BoneWeight();
				w.forceset("VertexId", LuaValue.valueOf(weight.mVertexId()));
				w.forceset("Weight", LuaValue.valueOf(weight.mWeight()));
				w.forceSetParent(b);
			}
			
			b.forceSetParent(boneData);
		}
	}
	
	public void processBoneTree(AINode node, Instance parent) {
		if ( parent == null ) {
			Instance boneTree = this.findFirstChild("BoneTree");
			if ( boneTree == null )
				boneTree = new BoneTree();
			boneTree.forceSetParent(this);
			parent = boneTree;
		}
		
		BoneTreeNode t = new BoneTreeNode();
		t.forceSetName(node.mName().dataString());
		
		for (int i = 0; i < node.mNumChildren(); i++) {
			AINode child = AINode.create(node.mChildren().get(i));
			processBoneTree( child, t );
		}
		
		t.forceSetParent(parent);
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

				Instance bones = this.findFirstChild("Bones");
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
					Matrix4f offsetMatrix = new Matrix4f();
					offsetMatrix.translate(
							entryK.getValue().position.mValue().x(),
							entryK.getValue().position.mValue().y(),
							entryK.getValue().position.mValue().z()
					);
					offsetMatrix.rotate(
							entryK.getValue().rotation.mValue().w(),
							entryK.getValue().rotation.mValue().x(),
							entryK.getValue().rotation.mValue().y(),
							entryK.getValue().rotation.mValue().z()
					);
					
					AnimationKeyframe keyframe = new AnimationKeyframe();
					keyframe.forceset("Bone", entryK.getKey());
					keyframe.forceset("Matrix", new Matrix4(offsetMatrix));
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
