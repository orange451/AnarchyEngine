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

package engine.glv2.renderers.shaders;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.MaterialGL;
import engine.gl.light.DirectionalLightInternal;
import engine.gl.light.PointLightInternal;
import engine.glv2.entities.CubeMapCamera;
import engine.glv2.shaders.ShaderProgram;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformBoolean;
import engine.glv2.shaders.data.UniformDirectionalLight;
import engine.glv2.shaders.data.UniformFloat;
import engine.glv2.shaders.data.UniformInteger;
import engine.glv2.shaders.data.UniformMaterial;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformPointLight;
import engine.glv2.shaders.data.UniformSampler;
import engine.glv2.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class InstanceFowardShader extends ShaderProgram {

	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformMaterial material = new UniformMaterial("material");
	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");
	private UniformVec3 lightPosition = new UniformVec3("lightPosition");
	private UniformSampler irradianceMap = new UniformSampler("irradianceMap");
	private UniformSampler preFilterEnv = new UniformSampler("preFilterEnv");
	private UniformSampler brdfLUT = new UniformSampler("brdfLUT");
	private UniformBoolean colorCorrect = new UniformBoolean("colorCorrect");

	private UniformVec3 uAmbient = new UniformVec3("uAmbient");
	private UniformFloat transparency = new UniformFloat("transparency");
	private UniformFloat gamma = new UniformFloat("gamma");
	private UniformFloat exposure = new UniformFloat("exposure");

	private UniformBoolean useShadows = new UniformBoolean("useShadows");

	private UniformMatrix4 projectionLightMatrix[];
	private UniformMatrix4 viewLightMatrix = new UniformMatrix4("viewLightMatrix");
	private UniformMatrix4 biasMatrix = new UniformMatrix4("biasMatrix");
	private UniformSampler shadowMap = new UniformSampler("shadowMap");

	private UniformPointLight pointLights[] = new UniformPointLight[8];
	private UniformInteger totalPointLights = new UniformInteger("totalPointLights");

	private UniformDirectionalLight directionalLights[] = new UniformDirectionalLight[8];
	private UniformInteger totalDirectionalLights = new UniformInteger("totalDirectionalLights");

	public InstanceFowardShader() {
		super("assets/shaders/renderers/InstanceForward.vs", "assets/shaders/renderers/InstanceForward.fs",
				new Attribute(0, "position"), new Attribute(1, "normals"), new Attribute(2, "textureCoords"),
				new Attribute(3, "inColor"));
		projectionLightMatrix = new UniformMatrix4[4];
		for (int x = 0; x < 4; x++)
			projectionLightMatrix[x] = new UniformMatrix4("projectionLightMatrix[" + x + "]");
		super.storeUniforms(projectionLightMatrix);
		for (int x = 0; x < 8; x++) {
			pointLights[x] = new UniformPointLight("pointLights[" + x + "]");
		}
		super.storeUniforms(pointLights);
		for (int x = 0; x < 8; x++) {
			directionalLights[x] = new UniformDirectionalLight("directionalLights[" + x + "]");
		}
		super.storeUniforms(directionalLights);
		super.storeUniforms(transformationMatrix, projectionMatrix, viewMatrix, material, cameraPosition, lightPosition,
				uAmbient, irradianceMap, preFilterEnv, brdfLUT, colorCorrect, biasMatrix, viewLightMatrix, useShadows,
				transparency, gamma, exposure, totalPointLights, shadowMap, totalDirectionalLights);
		super.validate();
		this.loadInitialData();
	}

	@Override
	protected void loadInitialData() {
		super.start();
		irradianceMap.loadTexUnit(4);
		preFilterEnv.loadTexUnit(5);
		brdfLUT.loadTexUnit(6);
		shadowMap.loadTexUnit(7);
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

	public void loadTransformationMatrix(Matrix4f matrix) {
		transformationMatrix.loadMatrix(matrix);
	}

	public void loadMaterial(MaterialGL mat) {
		this.material.loadMaterial(mat);
	}

	public void loadLightPosition(Vector3f lightPos) {
		lightPosition.loadVec3(lightPos);
	}

	public void colorCorrect(boolean val) {
		colorCorrect.loadBoolean(val);
	}

	public void loadCamera(Camera camera, Matrix4f projection) {
		projectionMatrix.loadMatrix(projection);
		viewMatrix.loadMatrix(camera.getViewMatrix().getInternal());
		cameraPosition.loadVec3(camera.getPosition().getInternal());
	}

	public void loadCamera(CubeMapCamera camera) {
		projectionMatrix.loadMatrix(camera.getProjectionMatrix());
		viewMatrix.loadMatrix(camera.getViewMatrix());
		cameraPosition.loadVec3(camera.getPosition());
	}

	public void loadBiasMatrix(Matrix4f[] shadowProjectionMatrix) {
		for (int x = 0; x < 4; x++)
			this.projectionLightMatrix[x].loadMatrix(shadowProjectionMatrix[x]);
	}

	public void loadPointLights(List<PointLightInternal> lights) {
		for (int x = 0; x < Math.min(8, lights.size()); x++)
			this.pointLights[x].loadLight(lights.get(x));
		totalPointLights.loadInteger(Math.min(8, lights.size()));
	}

	public void loadDirectionalLights(List<DirectionalLightInternal> lights) {
		synchronized (lights) {
			for (int x = 0; x < Math.min(8, lights.size()); x++)
				this.directionalLights[x].loadLight(lights.get(x), 8, x);
			totalDirectionalLights.loadInteger(Math.min(8, lights.size()));
		}
	}

	public void loadAmbient(Vector3f ambient) {
		this.uAmbient.loadVec3(ambient);
	}

	public void loadLightMatrix(Matrix4f sunCameraViewMatrix) {
		viewLightMatrix.loadMatrix(sunCameraViewMatrix);
	}

	public void loadSettings(boolean useShadows) {
		this.useShadows.loadBoolean(useShadows);
	}

	public void loadTransparency(float t) {
		transparency.loadFloat(t);
	}

	public void loadGamma(float t) {
		gamma.loadFloat(t);
	}

	public void loadExposure(float t) {
		exposure.loadFloat(t);
	}

}