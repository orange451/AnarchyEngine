/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import engine.gl.lights.SpotLightInternal;
import engine.gl.shaders.data.Attribute;
import engine.gl.shaders.data.UniformBoolean;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;
import engine.gl.shaders.data.UniformSpotLight;
import engine.gl.shaders.data.UniformVec2;
import engine.gl.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class SpotLightShader extends ShaderProgram {

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

	private UniformMatrix4 biasMatrix = new UniformMatrix4("biasMatrix");

	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");

	private UniformSpotLight light = new UniformSpotLight("light");

	private UniformBoolean useShadows = new UniformBoolean("useShadows");

	private UniformVec2 texel = new UniformVec2("texel");

	private Matrix4f projInv = new Matrix4f(), viewInv = new Matrix4f();

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/SpotLight.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/SpotLight.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, gDiffuse, gNormal, gDepth, gPBR, gMask, light,
				inverseProjectionMatrix, inverseViewMatrix, biasMatrix, useShadows, transformationMatrix, texel);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		gDiffuse.loadTexUnit(0);
		gNormal.loadTexUnit(2);
		gDepth.loadTexUnit(3);
		gPBR.loadTexUnit(4);
		gMask.loadTexUnit(5);
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

	public void loadSpotLight(SpotLightInternal l) {
		light.loadLight(l, 6);
	}

	public void loadTransformationMatrix(Matrix4f mat) {
		transformationMatrix.loadMatrix(mat);
	}

	public void loadUseShadows(boolean shadows) {
		useShadows.loadBoolean(shadows);
	}

	public void loadTexel(Vector2f texel) {
		this.texel.loadVec2(texel);
	}

	public void loadCameraData(Camera camera, Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
		this.viewMatrix.loadMatrix(camera.getViewMatrixInverse());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
		this.inverseProjectionMatrix.loadMatrix(projection.invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrixInverse().invert(viewInv));
	}
}
