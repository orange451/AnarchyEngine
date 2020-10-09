/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.compute.shaders;

import static org.lwjgl.opengl.GL45C.*;

import engine.gl.compute.BaseComputeShader;
import engine.gl.shaders.data.UniformSampler;

public class TAAShader extends BaseComputeShader {

	private UniformSampler image = new UniformSampler("image");
	private UniformSampler previous = new UniformSampler("previous");

	private UniformSampler gMotion = new UniformSampler("gMotion");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred_compute/TAA.comp", GL_COMPUTE_SHADER));
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
