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

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;

import org.joml.Matrix4f;

import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;
import engine.gl.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class LightingShader extends BasePipelineShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformMatrix4 inverseProjectionMatrix = new UniformMatrix4("inverseProjectionMatrix");
	private UniformMatrix4 inverseViewMatrix = new UniformMatrix4("inverseViewMatrix");

	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	private UniformSampler gDiffuse = new UniformSampler("gDiffuse");
	private UniformSampler gNormal = new UniformSampler("gNormal");
	private UniformSampler gDepth = new UniformSampler("gDepth");
	private UniformSampler gPBR = new UniformSampler("gPBR");
	private UniformSampler gMask = new UniformSampler("gMask");
	private UniformSampler irradianceCube = new UniformSampler("irradianceCube");
	private UniformSampler environmentCube = new UniformSampler("environmentCube");
	private UniformSampler brdfLUT = new UniformSampler("brdfLUT");
	private UniformSampler directionalLightData = new UniformSampler("directionalLightData");
	private UniformSampler pointLightData = new UniformSampler("pointLightData");
	private UniformSampler spotLightData = new UniformSampler("spotLightData");
	private UniformSampler areaLightData = new UniformSampler("areaLightData");

	private UniformMatrix4 biasMatrix = new UniformMatrix4("biasMatrix");

	private Matrix4f projInv = new Matrix4f(), viewInv = new Matrix4f();

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred/Lighting.fs", GL_FRAGMENT_SHADER));
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, gDiffuse, gNormal, gDepth, gPBR, gMask,
				irradianceCube, environmentCube, brdfLUT, biasMatrix, inverseProjectionMatrix, inverseViewMatrix,
				directionalLightData, pointLightData, spotLightData, areaLightData);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		gDiffuse.loadTexUnit(0);
		gNormal.loadTexUnit(1);
		gDepth.loadTexUnit(2);
		gPBR.loadTexUnit(3);
		gMask.loadTexUnit(4);
		irradianceCube.loadTexUnit(5);
		environmentCube.loadTexUnit(6);
		brdfLUT.loadTexUnit(7);
		directionalLightData.loadTexUnit(8);
		pointLightData.loadTexUnit(9);
		spotLightData.loadTexUnit(10);
		areaLightData.loadTexUnit(11);
		Matrix4f bias = new Matrix4f();
		bias.m00(0.5f);
		bias.m11(0.5f);
		bias.m22(0.5f);
		bias.m30(0.5f);
		bias.m31(0.5f);
		bias.m32(0.5f);
		biasMatrix.loadMatrix(bias);
		super.stop();
	}

	public void loadCameraData(Camera camera, Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
		this.viewMatrix.loadMatrix(camera.getViewMatrix().getInternal());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
		this.inverseProjectionMatrix.loadMatrix(projection.invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrix().getInternal().invert(viewInv));
	}
}
