/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.renderers.shaders;

import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.v2.lights.DirectionalLightCamera;

public class AnimInstanceDirectionalShadowShader extends AnimInstanceBaseShadowShader {

	private UniformMatrix4 projectionMatrix[] = new UniformMatrix4[4];
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/renderers/AnimInstanceDirectionalShadow.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/renderers/InstanceDirectionalShadow.gs", GL_GEOMETRY_SHADER));
		for (int i = 0; i < 4; i++)
			projectionMatrix[i] = new UniformMatrix4("projectionMatrix[" + i + "]");
		super.storeUniforms(projectionMatrix);
		super.storeUniforms(viewMatrix);
	}

	public void loadDirectionalLight(DirectionalLightCamera camera) {
		viewMatrix.loadMatrix(camera.getViewMatrix());
		for (int i = 0; i < 4; i++)
			projectionMatrix[i].loadMatrix(camera.getProjectionArray()[i]);
	}

}
