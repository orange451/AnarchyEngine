/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.mesh;

import java.util.Arrays;

import org.joml.Vector3f;

public class Vertex {
	// Vertex data
	private float[] v_xyz = new float[] {0f, 0f, 0f};
	private float[] n_xyz = new float[] {1f, 1f, 1f};
	private float[] rgba = new float[] {1f, 1f, 1f, 1f};
	private float[] st = new float[] {0f, 0f};

	// The amount of bytes an element has
	public static final int elementBytes = 4;

	// Elements per parameter
	public static final int positionElementCount = 3;
	public static final int normalElementCount = 3;
	public static final int textureElementCount = 2;
	public static final int colorElementCount = 4;

	// Bytes per parameter
	public static final int positionByteCount = positionElementCount * elementBytes;
	public static final int normalByteCount = normalElementCount * elementBytes;
	public static final int textureByteCount = textureElementCount * elementBytes;
	public static final int colorByteCount = colorElementCount * elementBytes;

	// Byte offsets per parameter
	public static final int positionByteOffset = 0;
	public static final int normalByteOffset = positionByteOffset + positionByteCount;
	public static final int textureByteOffset = normalByteOffset + normalByteCount;
	public static final int colorByteOffset = textureByteOffset + textureByteCount;

	// The amount of elements that a vertex has
	public static final int elementCount = positionElementCount + normalElementCount + textureElementCount + colorElementCount; 
	// The size of a vertex in bytes, like in C/C++: sizeof(Vertex)
	public static final int stride = positionByteCount + normalByteCount + textureByteCount + colorByteCount;

	public Vertex() {
		//
	}
	
	public Vertex(float x, float y, float z, float nx, float ny, float nz, float s, float t, float r, float g, float b, float a) {
		setXYZ(x, y, z);
		setNormalXYZ(nx, ny, nz);
		setRGBA(r, g, b, a);
		setST(s, t);
	}

	public Vertex(float x, float y, float z, float nx, float ny, float nz, float s, float t) {
		this(x, y, z, nx, ny, nz, s, t, 1, 1, 1, 1);
	}
	
	public Vertex( Vertex vertex ) {
		setXYZ(vertex.getXYZ()[0], vertex.getXYZ()[1], vertex.getXYZ()[2]);
		setNormalXYZ(vertex.getNormalXYZ()[0], vertex.getNormalXYZ()[1], vertex.getNormalXYZ()[2]);
		setRGBA(vertex.getRGBA()[0], vertex.getRGBA()[1], vertex.getRGBA()[2], vertex.getRGBA()[3]);
		setST(vertex.getST()[0], vertex.getST()[1]);
	}

	// Setters
	public Vertex setXYZ(float x, float y, float z) {
		this.v_xyz = new float[] { x, y, z};
		return this;
	}

	public Vertex setST(float s, float t) {
		this.st = new float[] {s, t};
		return this;
	}

	public Vertex setNormalXYZ(float x, float y, float z) {
		this.n_xyz = new float[] {x, y, z};
		return this;
	}

	public void setRGBA(float r, float g, float b, float a) {
		this.rgba = new float[] {r, g, b, a};
	}

	// Getters  
	public float[] getElements() {
		float[] out = new float[Vertex.elementCount];
		int i = 0;

		// Insert XYZ elements
		out[i++] = this.v_xyz[0];
		out[i++] = this.v_xyz[1];
		out[i++] = this.v_xyz[2];
		// Insert NORMAL elements
		out[i++] = this.n_xyz[0];
		out[i++] = this.n_xyz[1];
		out[i++] = this.n_xyz[2];
		// Insert ST elements
		out[i++] = this.st[0];
		out[i++] = this.st[1];
		// Insert RGBA elements
		out[i++] = this.rgba[0];
		out[i++] = this.rgba[1];
		out[i++] = this.rgba[2];
		out[i++] = this.rgba[3];

		return out;
	}

	public float[] ST() {
		return new float[] {this.v_xyz[0], this.v_xyz[1], this.v_xyz[2]};
	}

	public float[] getST() {
		return new float[] {this.st[0], this.st[1]};
	}

	public float[] getNormalXYZ() {
		return new float[] {this.n_xyz[0], this.n_xyz[1], this.n_xyz[2]};
	}

	public float[] getXYZ() {
		return new float[] {this.v_xyz[0], this.v_xyz[1], this.v_xyz[2]};
	}

	public float[] getRGBA() {
		return new float[] {this.rgba[0], this.rgba[1], this.rgba[2], this.rgba[3]};
	}

	public Vertex clone() {
		return new Vertex(v_xyz[0], v_xyz[1], v_xyz[2], n_xyz[0], n_xyz[1], n_xyz[2], st[0], st[1], rgba[0], rgba[1], rgba[2], rgba[3]);
	}

	public String toString() {
		return "(" + Float.toString(v_xyz[0]) + "," + Float.toString(v_xyz[1]) + "," + Float.toString(v_xyz[2]) + ")";
	}

	public boolean equalsLoose(Vertex vertex) {
		return Arrays.equals(v_xyz, vertex.v_xyz);
	}

	public Vertex add(Vector3f offset) {
		v_xyz[0] += offset.x;
		v_xyz[1] += offset.y;
		v_xyz[2] += offset.z;
		return this;
	}
}
