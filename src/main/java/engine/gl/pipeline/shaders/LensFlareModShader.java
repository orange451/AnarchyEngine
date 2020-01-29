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

public class LensFlareModShader extends BasePipelineShader {

	private UniformSampler lensDirt = new UniformSampler("lensDirt");
	private UniformSampler lensStar = new UniformSampler("lensStar");
	private UniformSampler lensFlare = new UniformSampler("lensFlare");
	private UniformSampler image = new UniformSampler("image");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred/LensFlaresMod.fs", GL_FRAGMENT_SHADER));
		super.storeUniforms(lensFlare, lensDirt, lensStar, image);
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
