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

import engine.glv2.entities.LayeredCubeCamera;
import engine.glv2.shaders.data.UniformMatrix4;

public class InstanceCubeShader extends InstanceFowardShader {

	private UniformMatrix4[] viewMatrixCube = new UniformMatrix4[6];

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/renderers/InstanceCube.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/renderers/InstanceCube.gs", GL_GEOMETRY_SHADER));
		for (int i = 0; i < 6; i++)
			viewMatrixCube[i] = new UniformMatrix4("viewMatrixCube[" + i + "]");
		super.storeUniforms(viewMatrixCube);
	}

	public void loadCamera(LayeredCubeCamera camera) {
		for (int i = 0; i < 6; i++)
			this.viewMatrixCube[i].loadMatrix(camera.getViewMatrix()[i]);
		super.projectionMatrix.loadMatrix(camera.getProjectionMatrix());
		super.cameraPosition.loadVec3(camera.getPosition());
	}

}
