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

package net.luxvacuos.lightengine.client.rendering.opengl.pipeline.shaders;

import java.util.List;

import org.joml.Matrix4f;

import net.luxvacuos.lightengine.client.ecs.entities.CameraEntity;
import net.luxvacuos.lightengine.client.rendering.opengl.shaders.data.UniformInteger;
import net.luxvacuos.lightengine.client.rendering.opengl.shaders.data.UniformLight;
import net.luxvacuos.lightengine.client.rendering.opengl.shaders.data.UniformMatrix;
import net.luxvacuos.lightengine.client.rendering.opengl.shaders.data.UniformSampler;
import net.luxvacuos.lightengine.client.rendering.opengl.shaders.data.UniformVec3;
import net.luxvacuos.lightengine.client.rendering.opengl.v2.lights.Light;

public class LocalLightsShader extends BasePipelineShader {

	private UniformMatrix projectionMatrix = new UniformMatrix("projectionMatrix");
	private UniformMatrix viewMatrix = new UniformMatrix("viewMatrix");
	private UniformMatrix inverseProjectionMatrix = new UniformMatrix("inverseProjectionMatrix");
	private UniformMatrix inverseViewMatrix = new UniformMatrix("inverseViewMatrix");

	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	private UniformLight lights[];
	private UniformInteger totalLights = new UniformInteger("totalLights");

	private UniformSampler gDiffuse = new UniformSampler("gDiffuse");
	private UniformSampler gPosition = new UniformSampler("gPosition");
	private UniformSampler gNormal = new UniformSampler("gNormal");
	private UniformSampler gDepth = new UniformSampler("gDepth");
	private UniformSampler gPBR = new UniformSampler("gPBR");
	private UniformSampler gMask = new UniformSampler("gMask");
	private UniformSampler image = new UniformSampler("image");

	private UniformMatrix biasMatrix = new UniformMatrix("biasMatrix");

	private Matrix4f projInv = new Matrix4f(), viewInv = new Matrix4f();

	public LocalLightsShader(String name) {
		super("DFR_" + name);
		lights = new UniformLight[26];
		for (int x = 0; x < 26; x++) {
			lights[x] = new UniformLight("lights[" + x + "]");
		}
		super.storeUniforms(lights);
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, gDiffuse, gPosition, gNormal, gDepth, gPBR,
				gMask, image, totalLights, biasMatrix, inverseProjectionMatrix, inverseViewMatrix);
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
		image.loadTexUnit(6);
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

	public void loadPointLightsPos(List<Light> lights) {
		for (int x = 0; x < lights.size(); x++)
			this.lights[x].loadLight(lights.get(x), 7, x);
		totalLights.loadInteger(lights.size());
	}

	public void loadCameraData(CameraEntity camera) {
		this.projectionMatrix.loadMatrix(camera.getProjectionMatrix());
		this.viewMatrix.loadMatrix(camera.getViewMatrix());
		this.cameraPosition.loadVec3(camera.getPosition());
		this.inverseProjectionMatrix.loadMatrix(camera.getProjectionMatrix().invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrix().invert(viewInv));
	}
}
