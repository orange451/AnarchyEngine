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

package engine.glv2.shaders;

import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformSampler;

public class FinalShader extends ShaderProgram {

	private UniformSampler image = new UniformSampler("image");

	public FinalShader(String name) {
		super("assets/shaders/" + name + ".vs", "assets/shaders/" + name + ".fs", new Attribute(0, "position"));
		super.storeUniforms(image);
		this.loadInitialData();
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		super.stop();
	}

}
