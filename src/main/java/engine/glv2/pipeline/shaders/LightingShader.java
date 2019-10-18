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

package engine.glv2.pipeline.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformSampler;
import engine.glv2.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class LightingShader extends BasePipelineShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformMatrix4 inverseProjectionMatrix = new UniformMatrix4("inverseProjectionMatrix");
	private UniformMatrix4 inverseViewMatrix = new UniformMatrix4("inverseViewMatrix");

	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	private UniformVec3 uAmbient = new UniformVec3("uAmbient");

	private UniformSampler gDiffuse = new UniformSampler("gDiffuse");
	private UniformSampler gPosition = new UniformSampler("gPosition");
	private UniformSampler gNormal = new UniformSampler("gNormal");
	private UniformSampler gDepth = new UniformSampler("gDepth");
	private UniformSampler gPBR = new UniformSampler("gPBR");
	private UniformSampler gMask = new UniformSampler("gMask");
	private UniformSampler volumetric = new UniformSampler("volumetric");
	private UniformSampler irradianceCube = new UniformSampler("irradianceCube");
	private UniformSampler environmentCube = new UniformSampler("environmentCube");
	private UniformSampler brdfLUT = new UniformSampler("brdfLUT");
	private UniformSampler directionalLightData = new UniformSampler("directionalLightData");
	private UniformSampler pointLightData = new UniformSampler("pointLightData");

	private UniformMatrix4 biasMatrix = new UniformMatrix4("biasMatrix");

	private Matrix4f projInv = new Matrix4f(), viewInv = new Matrix4f();

	public LightingShader(String name) {
		super("deferred/" + name);
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, uAmbient, gDiffuse, gPosition, gNormal,
				gDepth, gPBR, gMask, volumetric, irradianceCube, environmentCube, brdfLUT, biasMatrix,
				inverseProjectionMatrix, inverseViewMatrix, directionalLightData, pointLightData);
		super.validate();
		this.loadInitialData();
	}

	@Override
	protected void loadInitialData() {
		super.start();
		gDiffuse.loadTexUnit(0);
		gPosition.loadTexUnit(1);
		gNormal.loadTexUnit(2);
		gDepth.loadTexUnit(3);
		gPBR.loadTexUnit(4);
		gMask.loadTexUnit(5);
		volumetric.loadTexUnit(6);
		irradianceCube.loadTexUnit(7);
		environmentCube.loadTexUnit(8);
		brdfLUT.loadTexUnit(9);
		directionalLightData.loadTexUnit(11);
		pointLightData.loadTexUnit(12);
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

	public void loadAmbient(Vector3f ambient) {
		this.uAmbient.loadVec3(ambient);
	}

	public void loadCameraData(Camera camera, Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
		this.viewMatrix.loadMatrix(camera.getViewMatrix().getInternal());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
		this.inverseProjectionMatrix.loadMatrix(projection.invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrix().getInternal().invert(viewInv));
	}
}
