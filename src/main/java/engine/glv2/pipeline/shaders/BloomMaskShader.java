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

package engine.glv2.pipeline.shaders;

import engine.glv2.shaders.data.UniformSampler;

public class BloomMaskShader extends BasePipelineShader {

	private UniformSampler image = new UniformSampler("image");

	public BloomMaskShader(String name) {
		super("deferred/" + name);
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
