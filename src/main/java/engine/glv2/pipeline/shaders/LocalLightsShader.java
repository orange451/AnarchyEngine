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

import java.util.List;

import org.joml.Matrix4f;

import engine.gl.light.PointLightInternal;
import engine.glv2.shaders.data.UniformInteger;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformPointLight;
import engine.glv2.shaders.data.UniformSampler;
import engine.glv2.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class LocalLightsShader extends BasePipelineShader {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformMatrix4 inverseProjectionMatrix = new UniformMatrix4("inverseProjectionMatrix");
	private UniformMatrix4 inverseViewMatrix = new UniformMatrix4("inverseViewMatrix");

	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	private UniformPointLight lights[];
	private UniformInteger totalLights = new UniformInteger("totalLights");

	private UniformSampler gDiffuse = new UniformSampler("gDiffuse");
	private UniformSampler gPosition = new UniformSampler("gPosition");
	private UniformSampler gNormal = new UniformSampler("gNormal");
	private UniformSampler gDepth = new UniformSampler("gDepth");
	private UniformSampler gPBR = new UniformSampler("gPBR");
	private UniformSampler gMask = new UniformSampler("gMask");
	private UniformSampler image = new UniformSampler("image");

	private Matrix4f projInv = new Matrix4f(), viewInv = new Matrix4f();

	public LocalLightsShader(String name) {
		super("deferred/" + name);
		lights = new UniformPointLight[256];
		for (int x = 0; x < 256; x++) {
			lights[x] = new UniformPointLight("lights[" + x + "]");
		}
		super.storeUniforms(lights);
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, gDiffuse, gPosition, gNormal, gDepth, gPBR,
				gMask, image, totalLights, inverseProjectionMatrix, inverseViewMatrix);
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
		super.stop();
	}

	public void loadPointLightsPos(List<PointLightInternal> lights) {
		for (int x = 0; x < lights.size(); x++)
			this.lights[x].loadLight(lights.get(x));
		totalLights.loadInteger(lights.size());
	}

	public void loadCameraData(Camera camera, Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
		this.viewMatrix.loadMatrix(camera.getViewMatrix().getInternal());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
		this.inverseProjectionMatrix.loadMatrix(projection.invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrix().getInternal().invert(viewInv));
	}
}
