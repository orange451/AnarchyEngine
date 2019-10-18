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

import engine.glv2.shaders.data.UniformFloat;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformSampler;
import engine.glv2.shaders.data.UniformVec3;
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
		super("deferred/" + name);
		projectionLightMatrix = new UniformMatrix4[4];
		for (int x = 0; x < 4; x++)
			projectionLightMatrix[x] = new UniformMatrix4("projectionLightMatrix[" + x + "]");
		super.storeUniforms(projectionLightMatrix);
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, lightPosition, gPosition, gNormal, biasMatrix,
				viewLightMatrix, time, shadowMap);
		super.validate();
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
		this.viewMatrix.loadMatrix(camera.getViewMatrix().getInternal());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
	}

	public void loadTime(float time) {
		this.time.loadFloat(time);
	}

}
