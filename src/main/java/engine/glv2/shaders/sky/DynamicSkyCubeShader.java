/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.shaders.sky;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.entities.LayeredCubeCamera;
import engine.glv2.shaders.ShaderProgram;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformBoolean;
import engine.glv2.shaders.data.UniformDynamicSky;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformVec3;
import engine.lua.type.object.insts.DynamicSkybox;

public class DynamicSkyCubeShader extends ShaderProgram {

	private UniformMatrix4[] viewMatrixCube = new UniformMatrix4[6];
	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");

	private UniformVec3 lightPosition = new UniformVec3("lightPosition");
	private UniformBoolean renderSun = new UniformBoolean("renderSun");
	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	private UniformVec3 ambient = new UniformVec3("ambient");

	private UniformDynamicSky dynamicSky = new UniformDynamicSky("dynamicSky");

	private Matrix4f temp = new Matrix4f();

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/sky/DynamicCube.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/sky/DynamicCube.gs", GL_GEOMETRY_SHADER));
		super.addShader(new Shader("assets/shaders/sky/DynamicCube.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		for (int i = 0; i < 6; i++)
			viewMatrixCube[i] = new UniformMatrix4("viewMatrixCube[" + i + "]");
		super.storeUniforms(viewMatrixCube);
		super.storeUniforms(projectionMatrix, transformationMatrix, lightPosition, renderSun, cameraPosition,
				dynamicSky, ambient);
	}

	public void loadCamera(LayeredCubeCamera camera) {
		for (int i = 0; i < 6; i++) {
			temp.set(camera.getViewMatrix()[i]);
			temp._m30(0);
			temp._m31(0);
			temp._m32(0);
			this.viewMatrixCube[i].loadMatrix(temp);
		}
		this.projectionMatrix.loadMatrix(camera.getProjectionMatrix());
		cameraPosition.loadVec3(camera.getPosition());
	}

	public void loadTransformationMatrix(Matrix4f mat) {
		transformationMatrix.loadMatrix(mat);
	}

	public void loadLightPosition(Vector3f pos) {
		lightPosition.loadVec3(pos);
	}

	public void renderSun(boolean val) {
		renderSun.loadBoolean(val);
	}

	public void loadDynamicSky(DynamicSkybox sky) {
		dynamicSky.loadLight(sky);
	}

	public void loadAmbient(Vector3f ambient) {
		this.ambient.loadVec3(ambient);
	}

}
