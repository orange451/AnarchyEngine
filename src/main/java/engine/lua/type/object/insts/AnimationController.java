package engine.lua.type.object.insts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.InternalGameThread;
import engine.gl.Resources;
import engine.gl.shader.BaseShader;
import engine.lua.type.LuaConnection;
import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;

public class AnimationController extends Instance {
	
	protected ArrayList<AnimationTrack> playingAnimations;
	protected LuaConnection animationUpdator;

	protected final static LuaValue C_LINKED = LuaValue.valueOf("Linked");
	protected final static LuaValue C_PREFAB = LuaValue.valueOf("Prefab");
	
	private LuaConnection linkedConnection = null;
	private HashMap<Bone, Matrix4> boneAbsolutePositions;
	
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
		
		if ( boneAbsolutePositions != null )
			boneAbsolutePositions.clear();
		else
			boneAbsolutePositions = new HashMap<>(); 
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
	
	private void animate(double delta) {
		for (int i = 0; i < playingAnimations.size(); i++) {
			if ( i >= playingAnimations.size() )
				continue;
			
			AnimationTrack track = playingAnimations.get(i);
			if ( track == null )
				continue;
			
			track.update(delta);
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
	}

	/**
	 * Debug render
	 * @param shader
	 */
	public void debugRender(BaseShader shader) {
		
		GameObject linked = this.getLinkedInstance();
		if ( linked == null )
			return;
		
		Prefab prefab = linked.getPrefab();
		if ( prefab == null )
			return;
		
		Instance animationData = prefab.findFirstChildOfClass(AnimationData.class.getSimpleName());
		if ( animationData == null )
			return;
		
		Instance boneStructure = animationData.findFirstChildOfClass(BoneTree.class.getSimpleName());
		if ( boneStructure == null )
			return;
		
		Instance animations = animationData.findFirstChildOfClass(Animations.class.getSimpleName());
		if ( animations == null )
			return;
		
		Instance bones = animationData.findFirstChildOfClass(Bones.class.getSimpleName());
		if ( bones == null )
			return;
		
		for (int i = 0; i < playingAnimations.size(); i++) {
			AnimationTrack track = playingAnimations.get(i);
			AnimationKeyframeSequence keyframe = track.getCurrentKeyframe();
			
			if ( keyframe == null )
				continue;
			
			debugRenderRecursive( shader, keyframe, boneStructure, linked.getWorldMatrix().toJoml());
		}
	}
	
	private void debugRenderRecursive(BaseShader shader, AnimationKeyframeSequence keyframe, Instance root, Matrix4f parentMatrix) {
		Matrix4f globalTransformation = new Matrix4f(parentMatrix);
		
		// Do rendering
		if ( root instanceof BoneTreeNode ) {
			Instance keyframeBone = keyframe.findFirstChild(root.getName());
			if ( keyframeBone != null && keyframeBone instanceof AnimationKeyframe ) {
				Instance animationData = ((Instance)((Instance)((Instance)keyframe.getParent()).getParent()).getParent());
				Instance bones = animationData.findFirstChild("Bones");
				Instance bone = bones.findFirstChild(keyframeBone.getName());
				
				if ( bone != null && bone instanceof Bone ) {
					Matrix4f keyframeMatrix = ((AnimationKeyframe)keyframeBone).getMatrixInternal();
					Matrix4f offsetMatrix = ((Bone)bone).getOffsetMatrix().toJoml();
					Matrix4f inverseRoot = ((Matrix4)bones.get(Bones.C_ROOTINVERSE)).toJoml();
					
					globalTransformation.mul(keyframeMatrix);
					
					Matrix4f finalTransform = new Matrix4f();
					finalTransform.mul(inverseRoot);
					finalTransform.mul(globalTransformation);
					//finalTransform.mul(offsetMatrix.invert());
					
					Resources.MESH_CUBE.render(shader, finalTransform, Resources.MATERIAL_BLANK);
					
					boneAbsolutePositions.put((Bone) bone, new Matrix4(finalTransform));
				}
			}
		}
		
		// Render children
		List<Instance> bones = root.getChildren();
		for (int i = 0; i < bones.size(); i++) {
			Instance newRoot = bones.get(i);
			debugRenderRecursive( shader, keyframe, newRoot, globalTransformation );
		}
	}

}
