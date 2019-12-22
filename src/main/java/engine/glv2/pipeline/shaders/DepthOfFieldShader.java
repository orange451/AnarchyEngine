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

import engine.glv2.shaders.data.UniformSampler;

public class DepthOfFieldShader extends BasePipelineShader {

	private UniformSampler image = new UniformSampler("image");
	private UniformSampler depth = new UniformSampler("depth");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/postprocess/DoF.fs", GL_FRAGMENT_SHADER));
		super.storeUniforms(image, depth);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		depth.loadTexUnit(1);
		super.stop();
	}

}
