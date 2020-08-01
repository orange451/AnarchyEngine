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

public class MergeReflectionShader extends BasePipelineShader {

	private UniformSampler gMask = new UniformSampler("gMask");
	private UniformSampler baseTex = new UniformSampler("baseTex");
	private UniformSampler ssrTex = new UniformSampler("ssrTex");
	private UniformSampler reflectionTex = new UniformSampler("reflectionTex");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred/MergeReflection.fs", GL_FRAGMENT_SHADER));
		super.storeUniforms(gMask, baseTex, ssrTex, reflectionTex);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		gMask.loadTexUnit(0);
		baseTex.loadTexUnit(1);
		ssrTex.loadTexUnit(2);
		reflectionTex.loadTexUnit(3);
		super.stop();
	}

}
