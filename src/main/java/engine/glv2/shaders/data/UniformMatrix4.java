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

import static org.lwjgl.opengl.GL20C.glUniformMatrix4fv;

import org.joml.Matrix4f;

public class UniformMatrix4 extends Uniform {

	private Matrix4f current;
	private float[] fm = new float[16];

	public UniformMatrix4(String name) {
		super(name);
	}

	public void loadMatrix(Matrix4f matrix) {
		if (!used || !matrix.equals(current)) {
			matrix.get(fm);
			glUniformMatrix4fv(super.getLocation(), false, fm);
		}
	}

}
