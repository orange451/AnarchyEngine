/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.InternalGameThread;
import engine.gl.mesh.animation.AnimatedModel;
import engine.gl.shader.BaseShader;
import engine.lua.type.LuaConnection;
import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Prefab;

public class AnimationController extends Instance {
	
	protected ArrayList<AnimationTrack> playingAnimations;
	protected LuaConnection animationUpdator;

	protected final static LuaValue C_LINKED = LuaValue.valueOf("Linked");
	protected final static LuaValue C_PREFAB = LuaValue.valueOf("Prefab");
	
	private LuaConnection linkedConnection = null;
	private HashMap<String, Matrix4> boneAbsolutePositions;
	private HashMap<String, Matrix4f> boneNameToPreviousTransformationMap;
	private HashMap<String, Matrix4f> boneNameToPreviousRootInverseMap;
	private AnimatedModel animatedModel;
	
	public AnimationController() {
		super("AnimationController");
		
		this.defineField(C_LINKED.toString(), LuaValue.NIL, true);
		
		this.playingAnimations = new ArrayList<AnimationTrack>();
		
		this.getmetatable().set("LoadAnimation", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg1) {
				if ( arg1.isnil() || !(arg1 instanceof Animation) )
					return LuaValue.NIL;
				
				AnimationTrack track = new AnimationTrack(AnimationController.this, (Animation) arg1);
				return track;
			}
		});
		
		// Handle animations
		InternalGameThread.runLater(()->{
			animationUpdator = Game.runService().heartbeatEvent().connect((args)->{
				animate(args[0].checkdouble());
			});
		});
		
		// Reset animations if linked changes... Also reset animations of the linked prefab changes?
		this.changedEvent().connect((args) -> {
			if ( args[0].eq_b(C_LINKED) ) {
				clearAnimations();
				
				if ( linkedConnection != null ) {
					linkedConnection.disconnect();
					linkedConnection = null;
				}
				
				LuaValue val = this.get(args[0]);
				if ( !val.isnil() ) {
					linkedConnection = ((Instance)val).changedEvent().connect((args2)->{
						if ( args2[0].eq_b(C_PREFAB) ) {
							clearAnimations();
						}
					});
				}
			}
		});
	}

	private void clearAnimations() {
		playingAnimations.clear();
		System.out.println("Animations cleared");
		
		// Clear bone absolute positions
		if ( boneAbsolutePositions != null )
			boneAbsolutePositions.clear();
		else
			boneAbsolutePositions = new HashMap<>(); 
		
		// clear previous transformations
		if ( boneNameToPreviousTransformationMap != null )
			boneNameToPreviousTransformationMap.clear();
		else
			boneNameToPreviousTransformationMap = new HashMap<>();	
		
		// clear previous root index
		if ( boneNameToPreviousRootInverseMap != null )
			boneNameToPreviousRootInverseMap.clear();
		else
			boneNameToPreviousRootInverseMap = new HashMap<>();	
		
		animatedModel = new AnimatedModel(this);
	}

	@Override
	public void onDestroy() {
		if ( animationUpdator != null )
			animationUpdator.disconnect();
	}

	@Override
	public void onValueUpdated(LuaValue key, LuaValue value) {
		if ( key.eq_b(C_PARENT) ) {
			if ( value.isnil() || !(value instanceof GameObject) ) {
				this.forceset(C_LINKED, LuaValue.NIL);
			} else {
				this.forceset(C_LINKED, value);
			}
		}
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	private static final LuaValue C_ANIMATIONDATA = LuaValue.valueOf("AnimationData");
	private static final LuaValue C_ANIMATIONS = LuaValue.valueOf("Animations");
	private static final LuaValue C_BONETREE = LuaValue.valueOf("BoneTree");
	private static final LuaValue C_BONES = LuaValue.valueOf("Bones");
	
	private void animate(double delta) {
		GameObject linked = this.getLinkedInstance();
		if ( linked == null )
			return;
		
		Prefab prefab = linked.getPrefab();
		if ( prefab == null )
			return;
		
		Instance animationData = prefab.findFirstChildOfClass(C_ANIMATIONDATA);
		if ( animationData == null )
			return;
		
		Instance boneStructure = animationData.findFirstChildOfClass(C_BONETREE);
		if ( boneStructure == null )
			return;
		
		Instance animations = animationData.findFirstChildOfClass(C_ANIMATIONS);
		if ( animations == null )
			return;
		
		Instance bones = animationData.findFirstChildOfClass(C_BONES);
		if ( bones == null )
			return;
		
		if ( boneStructure.getChildrenSize() == 0 )
			return;
		
		for (int i = 0; i < playingAnimations.size(); i++) {
			if ( i >= playingAnimations.size() )
				continue;
			
			// Get current playing track
			AnimationTrack track = playingAnimations.get(i);
			if ( track == null )
				continue;
			
			// Update it
			track.update(delta);
			
			// Get the current keyframe in trrack
			AnimationKeyframeSequence keyframe = track.getCurrentKeyframe();
			if ( keyframe == null )
				continue;
			
			// Make sure there's children
			List<Instance> children = boneStructure.getChildren();
			if ( children == null || children.size() == 0 )
				continue;
			
			Instance firstBone = children.get(0);
			if ( firstBone == null )
				continue;
			
			computeBones( keyframe, firstBone, new Matrix4f().translate(linked.getWorldMatrix().toJoml().getTranslation(new Vector3f())));
		}
	}
	
	/**
	 * Returns the Game Object this controller is linked to. Should be the same as the parent.
	 * @return
	 */
	public GameObject getLinkedInstance() {
		LuaValue linked = this.get(C_LINKED);
		return linked.isnil()?null:(GameObject)linked;
	}

	/**
	 * Plays an animation track
	 * @param track
	 */
	public void playAnimation(AnimationTrack track) {
		while ( playingAnimations.contains(track) )
			playingAnimations.remove(track);
		
		playingAnimations.add(track);
		
		AnimationKeyframeSequence frame = track.getAnimation().getNearestSequenceBefore(track.getTimePosition());
		if ( frame != null ) {
			track.setCurrentKeyframe(frame);
		}
	}
	
	/**
	 * Returns the amount of animations playing in this controller.
	 * @return
	 */
	public int getPlayingAnimations() {
		return this.playingAnimations.size();
	}

	public void debugRender(BaseShader shader) {		
	}
	
	private static final Matrix4f IDENTITY = new Matrix4f().identity();
	private void computeBones(AnimationKeyframeSequence keyframe, Instance root, Matrix4f parentMatrix) {
		Matrix4f globalTransformation = new Matrix4f(parentMatrix);
		String boneName = root.getName();
		
		// Check if this bone has a keyframe
		if ( root instanceof BoneTreeNode ) {
			Instance keyframeBone = keyframe.findFirstChild(root.getName());
			if ( keyframeBone != null && keyframeBone instanceof AnimationKeyframe ) {
				Instance animationData = ((Instance)((Instance)((Instance)keyframe.getParent()).getParent()).getParent());
				Instance bones = animationData.findFirstChild("Bones");
				Instance bone = bones.findFirstChild(keyframeBone.getName());
				
				if ( bone != null && bone instanceof Bone ) {
					boneName = bone.getName();
					
					boneNameToPreviousTransformationMap.put(boneName, ((AnimationKeyframe)keyframeBone).getMatrixInternal());
					boneNameToPreviousRootInverseMap.put(boneName, ((Matrix4)bones.get(Bones.C_ROOTINVERSE)).getInternal());
				}
			}
		}
		
		// Get the previously defined local-transformation for the bone)
		// IDENTITY if it is not yet defined.
		Matrix4f previousRootTransformation = boneNameToPreviousTransformationMap.get(boneName);
		if ( previousRootTransformation == null )
			previousRootTransformation = IDENTITY;
		
		// Get the previously defined root inverse (normally does not change per animation)
		// IDENTITY if it is not yet defined. It's most likely to be IDENTITY anyways...
		Matrix4f previousRootInverse = boneNameToPreviousRootInverseMap.get(boneName);
		if ( previousRootInverse == null )
			previousRootInverse = IDENTITY;
		
		// Update this bones matrix
		globalTransformation.mul(previousRootTransformation);
		Matrix4f finalTransform = new Matrix4f();
		finalTransform.mul(previousRootInverse);
		finalTransform.mul(globalTransformation);
		boneAbsolutePositions.put(boneName, new Matrix4(finalTransform));
		
		// Update children
		List<Instance> bones = root.getChildren();
		for (int i = 0; i < bones.size(); i++) {
			Instance newRoot = bones.get(i);
			computeBones( keyframe, newRoot, globalTransformation );
		}
	}

	public HashMap<String, Matrix4> getBoneAbsolutePositions() {
		return boneAbsolutePositions;
	}

	public AnimatedModel getAnimatedModel() {
		return animatedModel;
	}

}