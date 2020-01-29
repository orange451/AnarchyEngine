/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.shaders.sky;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.entities.CubeMapCamera;
import engine.gl.shaders.ShaderProgram;
import engine.gl.shaders.data.Attribute;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class AmbientSkyShader extends ShaderProgram {

	protected UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");

	private UniformVec3 ambient = new UniformVec3("ambient");

	private UniformMatrix4 viewMatrixPrev = new UniformMatrix4("viewMatrixPrev");
	private UniformMatrix4 projectionMatrixPrev = new UniformMatrix4("projectionMatrixPrev");

	private Matrix4f temp = new Matrix4f();

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/sky/Ambient.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/sky/Ambient.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		super.storeUniforms(projectionMatrix, transformationMatrix, viewMatrix, ambient, viewMatrixPrev,
				projectionMatrixPrev);
	}

	public void loadCamera(Camera camera, Matrix4f projection) {
		projectionMatrix.loadMatrix(projection);
		temp.set(camera.getViewMatrix().getInternal());
		temp._m30(0);
		temp._m31(0);
		temp._m32(0);
		viewMatrix.loadMatrix(temp);
	}

	public void loadCamera(CubeMapCamera camera) {
		projectionMatrix.loadMatrix(camera.getProjectionMatrix());
		temp.set(camera.getViewMatrix());
		temp._m30(0);
		temp._m31(0);
		temp._m32(0);
		viewMatrix.loadMatrix(temp);
	}

	public void loadCameraPrev(Matrix4f viewMatrixPrev, Matrix4f projectionMatrixPrev) {
		temp.set(viewMatrixPrev);
		temp._m30(0);
		temp._m31(0);
		temp._m32(0);
		this.viewMatrixPrev.loadMatrix(temp);
		this.projectionMatrixPrev.loadMatrix(projectionMatrixPrev);
	}

	public void loadTransformationMatrix(Matrix4f mat) {
		transformationMatrix.loadMatrix(mat);
	}

	public void loadAmbient(Vector3f ambient) {
		this.ambient.loadVec3(ambient);
	}

}
