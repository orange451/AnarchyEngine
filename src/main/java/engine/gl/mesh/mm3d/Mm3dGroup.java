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

import java.nio.ByteBuffer;

public class Mm3dGroup {
	private int FLAGS;
	private String NAME;
	private int TRIANGLES;
	private int[] TRIANGLE_INDICES;
	private int SMOOTHNESS;
	private int MATERIAL;
	
	public Mm3dGroup(int a, String b, int c, int[] d, int e, int f) {
		this.FLAGS = a;
		this.NAME = b;
		this.TRIANGLES = c;
		this.TRIANGLE_INDICES = d;
		this.SMOOTHNESS = e;
		this.MATERIAL = f;
	}
	
	public String getName() {
		return this.NAME;
	}
	
	public int getTriangles() {
		return this.TRIANGLES;
	}
	
	public int[] getIndices() {
		return this.TRIANGLE_INDICES;
	}
	
	public int getMaterial() {
		return this.MATERIAL;
	}

}
