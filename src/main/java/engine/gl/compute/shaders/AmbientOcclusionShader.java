/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.compute.shaders;

import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

import org.joml.Matrix4f;

import engine.gl.compute.BaseComputeShader;
import engine.gl.shaders.data.UniformFloat;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;
import engine.gl.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class AmbientOcclusionShader extends BaseComputeShader {

	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformMatrix4 inverseProjectionMatrix = new UniformMatrix4("inverseProjectionMatrix");
	private UniformMatrix4 inverseViewMatrix = new UniformMatrix4("inverseViewMatrix");

	private UniformSampler gNormal = new UniformSampler("gNormal");
	private UniformSampler gDepth = new UniformSampler("gDepth");
	private UniformSampler gMask = new UniformSampler("gMask");
	private UniformSampler gMotion = new UniformSampler("gMotion");

	private UniformSampler directionalLightData = new UniformSampler("directionalLightData");
	private UniformSampler pointLightData = new UniformSampler("pointLightData");
	private UniformSampler spotLightData = new UniformSampler("spotLightData");
	private UniformSampler areaLightData = new UniformSampler("areaLightData");

	private UniformSampler voxelImage = new UniformSampler("voxelImage");
	private UniformFloat voxelSize = new UniformFloat("voxelSize");
	private UniformFloat voxelOffset = new UniformFloat("voxelOffset");

	private Matrix4f projInv = new Matrix4f();

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred_compute/AmbientOcclusion.comp", GL_COMPUTE_SHADER));
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, gNormal, gDepth, gMask, gMotion,
				directionalLightData, pointLightData, spotLightData, areaLightData, voxelImage, voxelSize, voxelOffset,
				inverseProjectionMatrix, inverseViewMatrix);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		gNormal.loadTexUnit(0);
		gDepth.loadTexUnit(1);
		gMask.loadTexUnit(2);
		gMotion.loadTexUnit(3);
		directionalLightData.loadTexUnit(4);
		pointLightData.loadTexUnit(5);
		spotLightData.loadTexUnit(6);
		areaLightData.loadTexUnit(7);
		voxelImage.loadTexUnit(8);
		super.stop();
	}

	public void loadCameraData(Camera camera, Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
		this.viewMatrix.loadMatrix(camera.getViewMatrixInternal());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
		this.inverseProjectionMatrix.loadMatrix(projection.invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrixInverseInternal());
	}

	public void loadVoxelSize(float size) {
		voxelSize.loadFloat(size);
	}

	public void loadVoxelOffset(float offset) {
		voxelOffset.loadFloat(offset);
	}

}
