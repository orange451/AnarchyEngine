/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
