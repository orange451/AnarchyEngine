/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.renderers.shaders;

import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

import engine.gl.entities.LayeredCubeCamera;
import engine.gl.shaders.data.UniformMatrix4;

public class InstancePointShadowShader extends InstanceBaseShadowShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4[] viewMatrixCube = new UniformMatrix4[6];

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/renderers/InstancePointShadow.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/renderers/InstancePointShadow.gs", GL_GEOMETRY_SHADER));
		for (int i = 0; i < 6; i++)
			viewMatrixCube[i] = new UniformMatrix4("viewMatrixCube[" + i + "]");
		super.storeUniforms(viewMatrixCube);
		super.storeUniforms(projectionMatrix);
	}

	public void loadPointLight(LayeredCubeCamera camera) {
		for (int i = 0; i < 6; i++)
			this.viewMatrixCube[i].loadMatrix(camera.getViewMatrix()[i]);
		this.projectionMatrix.loadMatrix(camera.getProjectionMatrix());
	}

}
