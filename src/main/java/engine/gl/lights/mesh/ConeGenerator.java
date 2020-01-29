/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.lights.mesh;

import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

import org.joml.Vector3f;

import engine.gl.objects.VAO;

public class ConeGenerator {

	public static VAO create(int subdivisions) {
		Vector3f[] positions = new Vector3f[subdivisions + 1];

		positions[0] = new Vector3f(0, 0, 0);

		for (int i = 0; i < subdivisions; i++) {
			positions[i + 1] = new Vector3f((float) Math.cos(Math.PI * 2 * i / subdivisions),
					(float) Math.sin(Math.PI * 2 * i / subdivisions), 1);
		}

		int[] indices = new int[(subdivisions * 2 - 2) * 3];

		int index = 0;
		for (int i = 1; i < subdivisions; i++) {
			indices[index++] = 0; // Source point
			indices[index++] = i + 1;
			indices[index++] = i;
		}
		indices[index++] = 0; // Source point
		indices[index++] = 1;
		indices[index++] = subdivisions;

		for (int i = 2; i < subdivisions; i++) {
			indices[index++] = 1;
			indices[index++] = i;
			indices[index++] = i + 1;
		}

		float[] verticesArray = new float[positions.length * 3];

		for (int i = 0; i < positions.length; i++) {
			Vector3f position = positions[i];
			verticesArray[i * 3] = position.x;
			verticesArray[i * 3 + 1] = position.y;
			verticesArray[i * 3 + 2] = position.z;
		}

		VAO vao = VAO.create();
		vao.bind();
		vao.createIndexBuffer(indices, GL_STATIC_DRAW);
		vao.createAttribute(0, verticesArray, 3, GL_STATIC_DRAW);
		vao.unbind();
		return vao;
	}
}