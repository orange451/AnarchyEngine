package engine.gl.mesh.animation;

import java.util.ArrayList;

public class Animation {
	private float speed;
	private float overrideSpeed;
	private float frame_current;
	private boolean loop = true;
	private AnimatedModel parent;
	private String name;

	protected ArrayList<AnimationFrame> frames;

	public Animation( AnimatedModel parent, String name, float speed, boolean loop ) {
		this.parent = parent;
		this.name = name;
		this.speed = speed;
		this.loop = loop;
		this.frames = new ArrayList<AnimationFrame>();
		this.overrideSpeed = speed;
	}


	/**
	 * Return the current frame of the animation as an integer.
	 * @return
	 */
	public int getCurrent() {
		return (int) frame_current;
	}
	
	
	/**
	 * Return whether this animation loops
	 * @return
	 */
	public boolean doesLoop() {
		return this.loop;
	}
	
	
	/**
	 * Sets whether this animation loops.
	 * @param loop
	 */
	public void setLoop( boolean loop ) {
		this.loop = loop;
	}


	/**
	 * Return the exact frame value of the animation.
	 * @return
	 */
	public float getExactFrame() {
		return frame_current;
	}


	/**
	 * This method steps along the animation by a delta time.
	 * @param delta
	 */
	public void tick( float delta ) {
		frame_current += overrideSpeed * delta;
		overrideSpeed = speed;
		
		if (frame_current >= frames.size()) {
			parent.onAnimationEnd(this);
			if ( loop ) {
				frame_current = 0;
			} else {
				frame_current = frames.size();
			}
		}
	}


	/**
	 * Return an object representing the current frame.
	 * @return
	 */
	public AnimationFrame getCurrentFrame() {
		if (frames.size() == 0)
			return null;
		
		int c = getCurrent();
		if (c > frames.size() - 1)
			c = frames.size() - 1;

		return frames.get( c );
	}


	/**
	 * Restart the animation.
	 */
	public void reset() {
		frame_current = 0;
	}


	/**
	 * This method returns the name of the animation.
	 * @return
	 */
	public String getName() {
		return name;
	}


	/**
	 * Returns the speed of this animation.
	 * @return
	 */
	public float getSpeed() {
		return this.speed;
	}


	public void overrideSpeed(float speed) {
		this.overrideSpeed = speed;
	}
}
