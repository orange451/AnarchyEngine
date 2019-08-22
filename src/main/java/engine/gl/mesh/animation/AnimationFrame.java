package engine.gl.mesh.animation;

import java.util.ArrayList;

public class AnimationFrame {
	public ArrayList<AnimationKeyFrame> keyframes;
	public Animation parent;
	
	public AnimationFrame(Animation parent) {
		this.parent = parent;
		this.keyframes = new ArrayList<AnimationKeyFrame>();
	}
}
