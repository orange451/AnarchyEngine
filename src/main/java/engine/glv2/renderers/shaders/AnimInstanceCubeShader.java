/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.renderers.shaders;

import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

import engine.glv2.entities.LayeredCubeCamera;
import engine.glv2.shaders.data.UniformMatrix4;

public class AnimInstanceCubeShader extends AnimInstanceFowardShader {
	
	private UniformMatrix4[] viewMatrixCube = new UniformMatrix4[6];

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/renderers/AnimInstanceCube.vs", GL_VERTEX_SHADER));
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