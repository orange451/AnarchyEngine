/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.pipeline.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.shaders.data.UniformFloat;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;
import engine.gl.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class VolumetricLightShader extends BasePipelineShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");

	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	private UniformVec3 lightPosition = new UniformVec3("lightPosition");

	private UniformFloat time = new UniformFloat("time");

	private UniformSampler gPosition = new UniformSampler("gPosition");
	private UniformSampler gNormal = new UniformSampler("gNormal");

	private UniformMatrix4 projectionLightMatrix[];
	private UniformMatrix4 viewLightMatrix = new UniformMatrix4("viewLightMatrix");
	private UniformMatrix4 biasMatrix = new UniformMatrix4("biasMatrix");
	private UniformSampler shadowMap = new UniformSampler("shadowMap");

	public VolumetricLightShader(String name) {
		//super("deferred/" + name);
		projectionLightMatrix = new UniformMatrix4[4];
		for (int x = 0; x < 4; x++)
			projectionLightMatrix[x] = new UniformMatrix4("projectionLightMatrix[" + x + "]");
		super.storeUniforms(projectionLightMatrix);
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, lightPosition, gPosition, gNormal, biasMatrix,
				viewLightMatrix, time, shadowMap);
		this.loadInitialData();
	}

	@Override
	protected void loadInitialData() {
		super.start();
		gPosition.loadTexUnit(0);
		gNormal.loadTexUnit(1);
		shadowMap.loadTexUnit(2);
		Matrix4f biasM = new Matrix4f();
		biasM.m00(0.5f);
		biasM.m11(0.5f);
		biasM.m22(0.5f);
		biasM.m30(0.5f);
		biasM.m31(0.5f);
		biasM.m32(0.5f);
		biasMatrix.loadMatrix(biasM);
		super.stop();
	}

	public void loadLightPosition(Vector3f pos) {
		lightPosition.loadVec3(pos);
	}

	public void loadCameraData(Camera camera, Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
		this.viewMatrix.loadMatrix(camera.getViewMatrixInternal());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
	}

	public void loadTime(float time) {
		this.time.loadFloat(time);
	}

}
