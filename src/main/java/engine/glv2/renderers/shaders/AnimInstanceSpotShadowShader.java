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

import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.v2.lights.SpotLightCamera;

public class AnimInstanceSpotShadowShader extends AnimInstanceBaseShadowShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/renderers/AnimInstanceSpotShadow.vs", GL_VERTEX_SHADER));
		super.storeUniforms(projectionMatrix);
	}

	public void loadSpotLight(SpotLightCamera camera) {
		viewMatrix.loadMatrix(camera.getViewMatrix());
		projectionMatrix.loadMatrix(camera.getProjectionMatrix());
	}

}
