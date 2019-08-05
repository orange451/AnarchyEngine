package engine.lua.type.object.insts;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.gl.Resources;
import engine.gl.shader.BaseShader;
import engine.lua.type.LuaConnection;
import engine.lua.type.object.Instance;

public class AnimationController extends Instance {
	
	protected ArrayList<AnimationTrack> playingAnimations;
	protected LuaConnection animationUpdator;
	
	public AnimationController() {
		super("AnimationController");
		
		this.defineField("Linked", LuaValue.NIL, true);
		
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
		if ( !Game.isLoaded() ) {
			Game.startEvent().connect((a)->{
				animationUpdator = Game.runService().heartbeatEvent().connect((args)->{
					animate(args[0].checkdouble());
				});
			});
		} else {
			animationUpdator = Game.runService().heartbeatEvent().connect((args)->{
				animate(args[0].checkdouble());
			});
		}
	}

	@Override
	public void onDestroy() {
		if ( animationUpdator != null )
			animationUpdator.disconnect();
	}

	@Override
	public void onValueUpdated(LuaValue key, LuaValue value) {
		if ( key.toString().equals("Parent") ) {
			if ( value.isnil() || !(value instanceof GameObject) ) {
				this.forceset("Linked", LuaValue.NIL);
			} else {
				this.forceset("Linked", value);
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
		LuaValue linked = this.get("Linked");
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
			
			debugRenderRecursive( shader, keyframe, boneStructure, linked.getWorldMatrix().getInternal());
		}
	}

	private void debugRenderRecursive(BaseShader shader, AnimationKeyframeSequence keyframe, Instance root, Matrix4f worldMatrix) {
		// Do rendering
		if ( root instanceof BoneTreeNode ) {
			Instance keyframeBone = keyframe.findFirstChild(root.getName());
			if ( keyframeBone != null && keyframeBone instanceof AnimationKeyframe ) {
				Instance bone = ((Instance)((Instance)((Instance)keyframe.getParent()).getParent()).getParent()).findFirstChild("Bones").findFirstChild(keyframeBone.getName());
				if ( bone != null && bone instanceof Bone ) {
					worldMatrix = new Matrix4f();
					worldMatrix.mul(((Bone)bone).getOffsetMatrix().getInternal());
					worldMatrix.mul(((AnimationKeyframe)keyframeBone).getMatrixJOML());
					Resources.MESH_CUBE.render(shader, worldMatrix, Resources.MATERIAL_BLANK);
				}
			}
		}
		
		// Render children
		List<Instance> bones = root.getChildren();
		for (int i = 0; i < bones.size(); i++) {
			Instance newRoot = bones.get(i);
			debugRenderRecursive( shader, keyframe, newRoot, worldMatrix );
		}
	}

}
