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

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import engine.gl.MaterialGL;
import engine.glv2.shaders.ShaderProgram;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformBoolean;
import engine.glv2.shaders.data.UniformMaterial;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.lua.type.object.insts.Camera;

public class InstanceDeferredShader extends ShaderProgram {

	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformMatrix4 jitterMatrix = new UniformMatrix4("jitterMatrix");
	private UniformMaterial material = new UniformMaterial("material");
	private UniformBoolean useTAA = new UniformBoolean("useTAA");

	private UniformMatrix4 transformationMatrixPrev = new UniformMatrix4("transformationMatrixPrev");
	private UniformMatrix4 viewMatrixPrev = new UniformMatrix4("viewMatrixPrev");
	private UniformMatrix4 projectionMatrixPrev = new UniformMatrix4("projectionMatrixPrev");

	private Matrix4f jitter = new Matrix4f();

	private int frameCont;

	// TODO: Move this
	private Vector2f sampleLocs[] = { new Vector2f(-7.0f, 1.0f).mul(1.0f / 8.0f),
			new Vector2f(-5.0f, -5.0f).mul(1.0f / 8.0f), new Vector2f(-1.0f, -3.0f).mul(1.0f / 8.0f),
			new Vector2f(3.0f, -7.0f).mul(1.0f / 8.0f), new Vector2f(5.0f, -1.0f).mul(1.0f / 8.0f),
			new Vector2f(7.0f, 7.0f).mul(1.0f / 8.0f), new Vector2f(1.0f, 3.0f).mul(1.0f / 8.0f),
			new Vector2f(-3.0f, 5.0f).mul(1.0f / 8.0f) };

	private Vector2f tmp = new Vector2f();

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/renderers/InstanceDeferred.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/renderers/InstanceDeferred.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"), new Attribute(1, "normals"),
				new Attribute(2, "textureCoords"), new Attribute(3, "inColor"));
		super.storeUniforms(transformationMatrix, material, projectionMatrix, viewMatrix, jitterMatrix, useTAA,
				transformationMatrixPrev, viewMatrixPrev, projectionMatrixPrev);
	}

	public void loadTransformationMatrix(Matrix4f matrix) {
		transformationMatrix.loadMatrix(matrix);
	}

	public void loadTransformationMatrixPrev(Matrix4f matrix) {
		transformationMatrixPrev.loadMatrix(matrix);
	}

	public void loadMaterial(MaterialGL mat) {
		material.loadMaterial(mat);
	}

	public void loadCameraPrev(Matrix4f viewMatrixPrev, Matrix4f projectionMatrixPrev) {
		this.viewMatrixPrev.loadMatrix(viewMatrixPrev);
		this.projectionMatrixPrev.loadMatrix(projectionMatrixPrev);
	}

	public void loadCamera(Camera camera, Matrix4f projection, Vector2f resolution, boolean taa) {
		projectionMatrix.loadMatrix(projection);
		viewMatrix.loadMatrix(camera.getViewMatrix().getInternal());
		useTAA.loadBoolean(taa);
		if (taa) {
			Vector2f texSize = new Vector2f(1.0f / resolution.x, 1.0f / resolution.y);

			Vector2f subsampleSize = texSize.mul(2.0f, new Vector2f());

			Vector2f S = sampleLocs[frameCont];

			Vector2f subsample = S.mul(subsampleSize, tmp);
			subsample.mul(0.5f);
			jitter.translation(subsample.x, subsample.y, 0);
			jitterMatrix.loadMatrix(jitter);
			frameCont++;
			frameCont %= 4;
		}
	}
}