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

public class LensFlareModShader extends BasePipelineShader {

	private UniformSampler lensDirt = new UniformSampler("lensDirt");
	private UniformSampler lensStar = new UniformSampler("lensStar");
	private UniformSampler lensFlare = new UniformSampler("lensFlare");
	private UniformSampler image = new UniformSampler("image");

	public LensFlareModShader(String name) {
		super("deferred/" + name);
		super.storeUniforms(lensFlare, lensDirt, lensStar, image);
		super.validate();
		this.loadInitialData();
	}

	@Override
	protected void loadInitialData() {
		super.start();
		lensFlare.loadTexUnit(0);
		lensDirt.loadTexUnit(1);
		lensStar.loadTexUnit(2);
		image.loadTexUnit(3);
		super.stop();
	}

}
