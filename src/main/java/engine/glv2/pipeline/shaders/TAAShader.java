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

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import engine.glv2.shaders.data.UniformSampler;

public class TAAShader extends BasePipelineShader {

	private UniformSampler image = new UniformSampler("image");
	private UniformSampler previous = new UniformSampler("previous");

	private UniformSampler gMotion = new UniformSampler("gMotion");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred/TAA.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/deferred/TAA.fs", GL_FRAGMENT_SHADER));
		this.storeUniforms(image, previous, gMotion);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		previous.loadTexUnit(1);
		gMotion.loadTexUnit(2);
		super.stop();
	}
}
