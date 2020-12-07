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

import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import engine.gl.compute.BaseComputeShader;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;
import engine.gl.shaders.data.UniformVec2;

public class AOBlurShader extends BaseComputeShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");

	private UniformSampler gDepth = new UniformSampler("gDepth");
	private UniformSampler gMask = new UniformSampler("gMask");
	private UniformSampler image = new UniformSampler("image");
	
	private UniformVec2 direction = new UniformVec2("direction");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred_compute/AOBlur.comp", GL_COMPUTE_SHADER));
		super.storeUniforms(projectionMatrix, gDepth, gMask, image, direction);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		gDepth.loadTexUnit(1);
		gMask.loadTexUnit(2);
		super.stop();
	}

	public void loadCameraData(Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
	}
	
	public void loadDirection(Vector2f direction) {
		this.direction.loadVec2(direction);
	}

}
