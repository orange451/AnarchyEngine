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

import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.v2.lights.DirectionalLightCamera;

public class InstanceDirectionalShadowShader extends InstanceBaseShadowShader {

	private UniformMatrix4 projectionMatrix[] = new UniformMatrix4[4];

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/renderers/InstanceDirectionalShadow.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/renderers/InstanceDirectionalShadow.gs", GL_GEOMETRY_SHADER));
		for (int i = 0; i < 4; i++)
			projectionMatrix[i] = new UniformMatrix4("projectionMatrix[" + i + "]");
		super.storeUniforms(projectionMatrix);
	}

	public void loadDirectionalLight(DirectionalLightCamera camera) {
		viewMatrix.loadMatrix(camera.getViewMatrix());
		for (int i = 0; i < 4; i++)
			projectionMatrix[i].loadMatrix(camera.getProjectionArray()[i]);
	}

}
