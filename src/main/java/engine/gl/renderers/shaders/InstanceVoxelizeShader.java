/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.renderers.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

import java.util.List;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.Maths;
import engine.gl.VoxelizedManager;
import engine.gl.lights.DirectionalLightInternal;
import engine.gl.lights.PointLightInternal;
import engine.gl.objects.MaterialGL;
import engine.gl.shaders.ShaderProgram;
import engine.gl.shaders.data.Attribute;
import engine.gl.shaders.data.UniformBoolean;
import engine.gl.shaders.data.UniformDirectionalLight;
import engine.gl.shaders.data.UniformInteger;
import engine.gl.shaders.data.UniformMaterial;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformPointLight;
import engine.gl.shaders.data.UniformSampler;
import engine.gl.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class InstanceVoxelizeShader extends ShaderProgram {

	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	private UniformMatrix4 projection = new UniformMatrix4("projection");
	private UniformMatrix4 viewX = new UniformMatrix4("viewX");
	private UniformMatrix4 viewY = new UniformMatrix4("viewY");
	private UniformMatrix4 viewZ = new UniformMatrix4("viewZ");

	private UniformMaterial material = new UniformMaterial("material");
	protected UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	private UniformBoolean useShadows = new UniformBoolean("useShadows");

	private UniformMatrix4 biasMatrix = new UniformMatrix4("biasMatrix");

	private UniformPointLight pointLights[] = new UniformPointLight[8];
	private UniformInteger totalPointLights = new UniformInteger("totalPointLights");

	private UniformDirectionalLight directionalLights[] = new UniformDirectionalLight[8];
	private UniformInteger totalDirectionalLights = new UniformInteger("totalDirectionalLights");

	private UniformSampler voxelImage = new UniformSampler("voxelImage");
	private UniformInteger resolution = new UniformInteger("resolution");

	private Matrix4f temp = new Matrix4f();
	private Vector3f tempVec = new Vector3f();

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/Voxelize.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/Voxelize.gs", GL_GEOMETRY_SHADER));
		super.addShader(new Shader("assets/shaders/Voxelize.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"), new Attribute(1, "normals"), new Attribute(2, "tangent"),
				new Attribute(3, "textureCoords"), new Attribute(4, "inColor"));
		for (int x = 0; x < 8; x++) {
			pointLights[x] = new UniformPointLight("pointLights[" + x + "]");
		}
		super.storeUniforms(pointLights);
		for (int x = 0; x < 8; x++) {
			directionalLights[x] = new UniformDirectionalLight("directionalLights[" + x + "]");
		}
		super.storeUniforms(directionalLights);
		super.storeUniforms(transformationMatrix, projection, viewX, viewY, viewZ, material, biasMatrix,
				totalPointLights, totalDirectionalLights, voxelImage, cameraPosition, useShadows, resolution);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		voxelImage.loadTexUnit(4);
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

	public void loadVoxelizationValues(VoxelizedManager vm, Camera camera) {
		projection.loadMatrix(vm.getProjectionMatrix());

		Vector3f cameraPos = new Vector3f();
		cameraPos.setComponent(0, Maths.round(camera.getPosition().getInternal().x(), vm.getCameraOffset()));
		cameraPos.setComponent(1, Maths.round(camera.getPosition().getInternal().y(), vm.getCameraOffset()));
		cameraPos.setComponent(2, Maths.round(camera.getPosition().getInternal().z(), vm.getCameraOffset()));

		cameraPosition.loadVec3(cameraPos);

		temp.identity();
		Maths.createViewMatrixRot(Math.toRadians(-90), Math.toRadians(90), Math.toRadians(90), temp);
		Maths.createViewMatrixPos(cameraPos.add(tempVec.set(vm.getCameraOffset(), 0, 0), new Vector3f()), temp);
		viewX.loadMatrix(temp);

		temp.identity();
		Maths.createViewMatrixRot(Math.toRadians(-90), Math.toRadians(0), Math.toRadians(0), temp);
		Maths.createViewMatrixPos(cameraPos.add(tempVec.set(0, vm.getCameraOffset(), 0), new Vector3f()), temp);
		viewY.loadMatrix(temp);

		temp.identity();
		Maths.createViewMatrixRot(Math.toRadians(-180), Math.toRadians(180), Math.toRadians(180), temp);
		Maths.createViewMatrixPos(cameraPos.add(tempVec.set(0, 0, 0), new Vector3f()), temp);
		viewZ.loadMatrix(temp);
		resolution.loadInteger(vm.getResolution());
	}

	public void loadPointLights(List<PointLightInternal> lights) {
		for (int x = 0; x < Math.min(8, lights.size()); x++)
			this.pointLights[x].loadLight(lights.get(x), 10);
		totalPointLights.loadInteger(Math.min(8, lights.size()));
	}

	public void loadDirectionalLights(List<DirectionalLightInternal> lights) {
		for (int x = 0; x < Math.min(8, lights.size()); x++)
			this.directionalLights[x].loadLight(lights.get(x), 8 + x);
		totalDirectionalLights.loadInteger(Math.min(8, lights.size()));
	}

	public void loadSettings(boolean useShadows) {
		this.useShadows.loadBoolean(useShadows);
	}

}