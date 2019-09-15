/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.shaders.data;

import static org.lwjgl.opengl.GL20C.glUniformMatrix3fv;

import org.joml.Matrix3f;

public class UniformMatrix3 extends Uniform {

	private Matrix3f current;
	private float[] fm = new float[9];

	public UniformMatrix3(String name) {
		super(name);
	}

	public void loadMatrix(Matrix3f matrix) {
		if (!used || !matrix.equals(current)) {
			matrix.get(fm);
			glUniformMatrix3fv(super.getLocation(), false, fm);
			used = true;
		}
	}

}
