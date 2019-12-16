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

package engine.glv2.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import org.joml.Matrix4f;

import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformSampler;

public class SphereToCubeShader extends ShaderProgram {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformSampler environmentMap = new UniformSampler("environmentMap");

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/SphereToCube.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/SphereToCube.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		super.storeUniforms(projectionMatrix, viewMatrix, environmentMap);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		environmentMap.loadTexUnit(0);
		super.stop();
	}

	public void loadviewMatrix(Matrix4f viewMatrix) {
		this.viewMatrix.loadMatrix(viewMatrix);
	}

	public void loadProjectionMatrix(Matrix4f projectionMatrix) {
		this.projectionMatrix.loadMatrix(projectionMatrix);
	}

}
