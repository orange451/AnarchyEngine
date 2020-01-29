/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.shaders.data;

import static org.lwjgl.opengl.GL20C.glUniformMatrix4fv;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;

public class UniformMatrix4 extends Uniform {

	private Matrix4f current = new Matrix4f();
	private float[] fm = new float[16];

	public UniformMatrix4(String name) {
		super(name);
	}

	public void loadMatrix(Matrix4f matrix) {
		if (!used || !matrix.equals(current)) {
			current.set(matrix);
			matrix.get(fm);
			glUniformMatrix4fv(super.getLocation(), false, fm);
			used = true;
		}
	}

	//TODO: Optimize
	public void loadMatrix(FloatBuffer matrix) {
		glUniformMatrix4fv(super.getLocation(), false, matrix);
	}

}
