package engine.gl.mesh.animation;

import org.joml.Vector3f;

public class AnimationKeyFrame {
	public Vector3f offset;
	private Bone bone;
	private AnimationKeyFrameType type;
	
	public AnimationKeyFrame( Bone bone, AnimationKeyFrameType type, Vector3f offset ) {
		this.bone = bone;
		this.type = type;
		this.offset = offset;
	}
	
	public Bone getBone() {
		return this.bone;
	}
	
	public AnimationKeyFrameType getType() {
		return this.type;
	}
	
	public static AnimationKeyFrame mergeKeyFrames( AnimationKeyFrame key1, AnimationKeyFrame key2, float merge ) {
		Vector3f off1 = new Vector3f( 0, 0, 0 );
		Vector3f off2 = new Vector3f( 0, 0, 0 );
		Bone bone = null;
		AnimationKeyFrameType type = AnimationKeyFrameType.ROTATION;
		
		if (key1 != null) {
			off1 = key1.offset;
			bone = key1.bone;
			type = key1.type;
		}
		if (key2 != null) {
			off2 = key2.offset;
			bone = key2.bone;
			type = key2.type;
		}
		
		Vector3f v = getMergeVector( off1, off2, merge );
		return new AnimationKeyFrame( bone, type, v );
	}
	
	private static Vector3f getMergeVector(Vector3f from, Vector3f to, float merge) {
		Vector3f tempVector = new Vector3f();
		tempVector.x = from.x + ((to.x - from.x) * merge);
		tempVector.y = from.y + ((to.y - from.y) * merge);
		tempVector.z = from.z + ((to.z - from.z) * merge);
		
		return tempVector;
	}
}
