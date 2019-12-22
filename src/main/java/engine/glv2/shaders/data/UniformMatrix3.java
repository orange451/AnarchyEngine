/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.shaders.data;

import static org.lwjgl.opengl.GL20C.glUniformMatrix3fv;

import org.joml.Matrix3f;

public class UniformMatrix3 extends Uniform {

	private Matrix3f current = new Matrix3f();
	private float[] fm = new float[9];

	public UniformMatrix3(String name) {
		super(name);
	}

	public void loadMatrix(Matrix3f matrix) {
		if (!used || !matrix.equals(current)) {
			current.set(matrix);
			matrix.get(fm);
			glUniformMatrix3fv(super.getLocation(), false, fm);
			used = true;
		}
	}

}
