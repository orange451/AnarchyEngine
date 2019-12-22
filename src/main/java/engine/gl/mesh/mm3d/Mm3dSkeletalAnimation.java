/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.mesh.mm3d;

import java.util.ArrayList;

public class Mm3dSkeletalAnimation {
	private long flags;
	private String name;
	public float fps;
	public boolean loop = true;
	private long frames;
	
	public ArrayList<Mm3dSkeletalFrame> animationFrames;
	
	public Mm3dSkeletalAnimation(long flags, String name, float fps, long l) {
		this.flags = flags;
		this.name = name;
		this.fps = fps;
		this.frames = l;
		this.animationFrames = new ArrayList<Mm3dSkeletalFrame>();
	}

	public long getFrames() {
		return this.frames;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * This method takes in all the animations keyframes and lerps them to calculate any missing data
	 */
	protected void smooth() {
		for (int i = 0; i < frames; i++) {
			Mm3dSkeletalFrame mframe = animationFrames.get(i);
			ArrayList<Mm3dKeyframe> keyframes = mframe.keyframes;
			for (int ii = 0; ii < keyframes.size(); ii++) {
				Mm3dKeyframe keyframe = keyframes.get(ii);
				Mm3dKeyframe nextKeyframe = getNextKeyframe(i, keyframe);
				int dist = getKeyframeFrame(nextKeyframe)-i;
				
				// If it doesn't repeat fix it
				if (nextKeyframe == null) {
					nextKeyframe = keyframe;
					dist = (int) (frames - i);
				}
				for (int iii = 1; iii < dist; iii++) {
					Mm3dSkeletalFrame newframe = animationFrames.get(i + iii);
					Mm3dKeyframe mergedKeyframe = Mm3dKeyframe.mergeKeyframes(keyframe, nextKeyframe, 1f/(float)(dist - (iii - 1)));
					newframe.keyframes.add(mergedKeyframe);
				}
			}
		}
	}
	
	private int getKeyframeFrame(Mm3dKeyframe keyFrame) {
		for (int i = 0; i < frames; i++) {
			Mm3dSkeletalFrame mframe = animationFrames.get(i);
			ArrayList<Mm3dKeyframe> keyframes = mframe.keyframes;
			for (int ii = 0; ii < keyframes.size(); ii++) {
				Mm3dKeyframe nextKeyframe = keyframes.get(ii);
				if (nextKeyframe.equals(keyFrame))
					return i;
			}
		}
		return -1;
	}

	private Mm3dKeyframe getNextKeyframe(int frame, Mm3dKeyframe keyframe) {
		for (int i = frame + 1; i < frames; i++) {
			if (i >= frames)
				continue;
			Mm3dSkeletalFrame mframe = animationFrames.get(i);
			ArrayList<Mm3dKeyframe> keyframes = mframe.keyframes;
			for (int ii = 0; ii < keyframes.size(); ii++) {
				if (keyframes.get(ii).getType() == keyframe.getType() && keyframes.get(ii).getJoint() == keyframe.getJoint()) {
					return keyframes.get(ii);
				}
			}
		}
		return null;
	}

	public long getFlags() {
		return flags;
	}

}
