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

public class Triangle {
	public Vertex[] vertices;
	public float[] texCoordS;
	public float[] texCoordT;
	public float[] normalX;
	public float[] normalY;
	public float[] normalZ;

	public Triangle() {
		this.vertices = new Vertex[3];
		this.texCoordS = new float[3];
		this.texCoordT = new float[3];
		this.normalX = new float[3];
		this.normalY = new float[3];
		this.normalZ = new float[3];
	}

	public Vertex getFinalVertex(int index) {
		Vertex src = vertices[index];
		Vertex ret = src.clone();
		ret.setST(texCoordS[index], texCoordT[index]);
		ret.setNormalXYZ(normalX[index], normalY[index], normalZ[index]);
		return ret;
	}

	public Vertex getVertex( int index ) {
		return vertices[index];
	}

	private int getIndex() {
		for (int i = 0; i < vertices.length; i++) {
			if ( vertices[i] == null ) {
				return i;
			}
		}
		return -1;
	}

	public void addVertex(Vertex vertex) {
		int index = getIndex();
		vertices[index] = vertex;
		texCoordS[index] = vertex.getST()[0];
		texCoordT[index] = vertex.getST()[1];
		normalX[index] = vertex.getNormalXYZ()[0];
		normalY[index] = vertex.getNormalXYZ()[1];
		normalZ[index] = vertex.getNormalXYZ()[2];
	}

	public boolean isFull() {
		return getIndex() == -1;
	}
}
