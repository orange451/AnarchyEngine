package engine.lua.type.object.insts;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.type.object.Instance;

public class AnimationTrack extends Instance {

	private AnimationController controller;
	private AnimationKeyframeSequence nextFrame;
	
	private static LuaValue C_ANIMATION = LuaValue.valueOf("Animation");
	private static LuaValue C_TIMEPOSITION = LuaValue.valueOf("TimePosition");
	private static LuaValue C_CURRENTKEYFRAME = LuaValue.valueOf("CurrentKeyframe");
	private static LuaValue C_LENGTH = LuaValue.valueOf("Length");
	private static LuaValue C_SPEED = LuaValue.valueOf("Speed");
	
	public AnimationTrack(AnimationController animationController, Animation animation) {
		super("AnimationTrack");
		
		double t = 0;
		AnimationKeyframeSequence current = animation.getNearestSequenceBefore(t);
		
		this.defineField(C_ANIMATION.toString(), animation, true);
		this.defineField(C_SPEED.toString(), LuaValue.valueOf(animation.getSpeed()), false);
		this.defineField("Looped", LuaValue.valueOf(animation.isLooped()), false);
		this.defineField(C_TIMEPOSITION.toString(), LuaValue.valueOf(t), false);
		this.defineField(C_CURRENTKEYFRAME.toString(), current==null?LuaValue.NIL:current, true);
		this.defineField(C_LENGTH.toString(), LuaValue.valueOf(animation.getMaxTime()), true);
		
		this.setArchivable(false);
		this.setInstanceable(false);
		
		this.controller = animationController;
		
		this.getmetatable().set("Play", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				if ( controller == null ) {
					throw new LuaError("Controller not specified for AnimationTrack");
				}
				controller.playAnimation(AnimationTrack.this);
				return LuaValue.NIL;
			}
		});
	}
	
	protected void update(double delta) {
		// Update the time
		this.forceset(C_TIMEPOSITION, this.rawget(C_TIMEPOSITION).add(delta*this.getSpeed()));
		
		// Get the current frame
		AnimationKeyframeSequence frame = this.getAnimation().getNearestSequenceBefore(this.getTimePosition());
		AnimationKeyframeSequence nextFrame = this.getAnimation().getNearestSequenceAfter(this.getTimePosition());

		// Handle updating the current frame
		if ( frame == null || !frame.equals(this.getCurrentKeyframe()) )
			this.forceset(C_CURRENTKEYFRAME, frame==null?LuaValue.NIL:frame);
		
		// Handle updating the next frame
		if ( nextFrame == null || !nextFrame.equals(this.getCurrentKeyframe()) )
			this.nextFrame = nextFrame;
		
		// Handle animation looping
		if ( this.getTimePosition() > this.getLength() ) {
			this.forceset(C_TIMEPOSITION, LuaValue.valueOf(0));
			update(0);
		}
	}
	
	public double getSpeed() {
		return this.rawget(C_SPEED).checkdouble();
	}
	
	/**
	 * Returns the length of the animation track in seconds.
	 * @return
	 */
	public double getLength() {
		return this.rawget(C_LENGTH).checkdouble();
	}
	
	/**
	 * Returns the current position of the animation track.
	 * @return
	 */
	public double getTimePosition() {
		return this.rawget(C_TIMEPOSITION).checkdouble();
	}
	
	/**
	 * Return the animation this animation track is attached to.
	 * @return
	 */
	public Animation getAnimation() {
		return (Animation) this.rawget(C_ANIMATION);
	}
	
	/**
	 * Returns the current keyframe sequence that this animation track is displaying.
	 * @return
	 */
	public AnimationKeyframeSequence getCurrentKeyframe() {
		LuaValue ret = this.rawget(C_CURRENTKEYFRAME);
		return ret.equals(LuaValue.NIL)?null:(AnimationKeyframeSequence)ret;
	}

	/**
	 * Sets the current keyframe sequence that this animation track will display.
	 * @param frame
	 */
	public void setCurrentKeyframe(AnimationKeyframeSequence frame) {
		if ( frame == null )
			this.rawset(C_CURRENTKEYFRAME, LuaValue.NIL);
		else
			this.rawset(C_CURRENTKEYFRAME, frame);
	}
	
	public AnimationKeyframeSequence getNextKeyframe() {
		return this.nextFrame;
	}
	
	@Override
	public void onDestroy() {
		//
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

}
