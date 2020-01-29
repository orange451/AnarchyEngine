/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.pipeline.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;

import engine.gl.shaders.data.UniformSampler;

public class LensFlaresShader extends BasePipelineShader {

	private UniformSampler bloom = new UniformSampler("bloom");
	private UniformSampler lensColor = new UniformSampler("lensColor");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred/LensFlares.fs", GL_FRAGMENT_SHADER));
		super.storeUniforms(bloom, lensColor);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		bloom.loadTexUnit(0);
		lensColor.loadTexUnit(1);
		super.stop();
	}

}
