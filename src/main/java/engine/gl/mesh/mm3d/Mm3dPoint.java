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

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Mm3dPoint {
	private long flags;
	private String name;
	private long type;
	private long joint;
	private float rot_x;
	private float rot_y;
	private float rot_z;
	private float trans_x;
	private float trans_y;
	private float trans_z;
	
	public Mm3dPoint(long flags, String name, long type, long joint, float rot_x, float rot_y, float rot_z, float trans_x, float trans_y, float trans_z ) {
		this.flags = flags;
		this.name = name;
		this.type = type;
		this.joint = joint;
		this.rot_x = rot_x;
		this.rot_y = rot_y;
		this.rot_z = rot_z;
		this.trans_x = trans_x;
		this.trans_y = trans_y;
		this.trans_z = trans_z;
	}

	public String getName() {
		return this.name;
	}
	
	public Vector3f getRotation() {
		return new Vector3f( rot_x, rot_y, rot_z );
	}
	
	public Vector3f getPosition() {
		return new Vector3f( trans_x, trans_y, trans_z );
	}

	public int getParent() {
		return (int) this.joint;
	}

	public Matrix4f getOffsetMatrix() {
		Matrix4f offset = new Matrix4f();
		offset.translate( getPosition() );
		Vector3f rot = getRotation();
		offset.rotateZYX(rot.z, rot.y, rot.x);
		//offset.rotate(getRotation().x, new Vector3f(1, 0, 0));
		//offset.rotate(getRotation().y, new Vector3f(0, 1, 0));
		//offset.rotate(getRotation().z, new Vector3f(0, 0, 1));
		return offset;
	}
}
