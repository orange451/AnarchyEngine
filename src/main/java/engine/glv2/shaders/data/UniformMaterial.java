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

import static org.lwjgl.opengl.GL20C.glUniform1f;
import static org.lwjgl.opengl.GL20C.glUniform1i;
import static org.lwjgl.opengl.GL20C.glUniform3f;

import engine.gl.MaterialGL;

public class UniformMaterial extends UniformArray {

	private MaterialGL currentValue;

	public UniformMaterial(String matName) {
		super(matName + ".diffuseTex", matName + ".normalTex", matName + ".metalnessTex", matName + ".roughnessTex",
				matName + ".metalness", matName + ".roughness", matName + ".reflective", matName + ".color",
				matName + ".emissive");
	}

	public void loadMaterial(MaterialGL value) {
		if (!used || !currentValue.equals(value)) {

			glUniform1i(super.getLocation()[0], 0);
			glUniform1i(super.getLocation()[1], 1);
			glUniform1i(super.getLocation()[2], 2);
			glUniform1i(super.getLocation()[3], 3);

			glUniform1f(super.getLocation()[4], value.getMetalness());
			glUniform1f(super.getLocation()[5], value.getRoughness());
			glUniform1f(super.getLocation()[6], value.getReflective());
			glUniform3f(super.getLocation()[7], value.getColor().x(), value.getColor().y(), value.getColor().z());
			glUniform3f(super.getLocation()[8], value.getEmissive().x(), value.getEmissive().y(),
					value.getEmissive().z());

			used = true;
			currentValue = value;
		}
	}

}
