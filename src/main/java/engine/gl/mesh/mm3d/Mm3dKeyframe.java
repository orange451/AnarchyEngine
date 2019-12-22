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

import org.joml.Vector3f;

public class Mm3dKeyframe {
	private int joint;
	private int type;
	private float pos_x;
	private float pos_y;
	private float pos_z;
	private Vector3f offset;
	
	public Mm3dKeyframe(long l, int type, float pos_x, float pos_y, float pos_z) {
		this.joint = (int) l;
		this.type = type;
		this.pos_x = pos_x;
		this.pos_y = pos_y;
		this.pos_z = pos_z;
		
		this.offset = new Vector3f(pos_x, pos_y, pos_z);
	}
	
	public int getJoint() {
		return this.joint;
	}

	public int getType() {
		return this.type;
	}
	
	public Vector3f getOffset() {
		return offset;
	}

	public static Mm3dKeyframe mergeKeyframes(Mm3dKeyframe keyframe, Mm3dKeyframe nextKeyframe, float merge) {
		Vector3f comb = getMergeVector(keyframe.getOffset(), nextKeyframe.getOffset(), merge);
		return new Mm3dKeyframe(keyframe.joint, keyframe.type, comb.x, comb.y, comb.z);
	}
	
	
	private static Vector3f tempVector = new Vector3f();
	private static Vector3f getMergeVector(Vector3f rot1, Vector3f rot2, float merge) {
		tempVector.x = rot1.x + ((rot2.x - rot1.x) * merge);
		tempVector.y = rot1.y + ((rot2.y - rot1.y) * merge);
		tempVector.z = rot1.z + ((rot2.z - rot1.z) * merge);
		
		return tempVector;
	}
	

}
