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
import org.joml.Vector4f;

import engine.gl.lights.AreaLightInternal;
import engine.gl.shaders.data.Attribute;
import engine.gl.shaders.data.UniformAreaLight;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;
import engine.gl.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;

public class AreaLightShader extends ShaderProgram {

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

	private UniformSampler ltcMag = new UniformSampler("ltcMag");
	private UniformSampler ltcMat = new UniformSampler("ltcMat");

	private UniformVec3[] points = new UniformVec3[4];

	private UniformAreaLight light = new UniformAreaLight("light");

	private Matrix4f projInv = new Matrix4f(), viewInv = new Matrix4f();

	private static final Vector4f temp = new Vector4f();

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/AreaLight.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/AreaLight.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		for (int x = 0; x < 4; x++)
			points[x] = new UniformVec3("points[" + x + "]");
		super.storeUniforms(points);
		super.storeUniforms(projectionMatrix, viewMatrix, cameraPosition, gDiffuse, gNormal, gDepth, gPBR, gMask, light,
				inverseProjectionMatrix, inverseViewMatrix, ltcMag, ltcMat);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		gDiffuse.loadTexUnit(0);
		gNormal.loadTexUnit(2);
		gDepth.loadTexUnit(3);
		gPBR.loadTexUnit(4);
		gMask.loadTexUnit(5);
		ltcMag.loadTexUnit(6);
		ltcMat.loadTexUnit(7);
		super.stop();
	}

	public void loadAreaLight(AreaLightInternal l) {
		light.loadLight(l);
	}

	public void loadPoints(Vector4f[] points, Matrix4f matrix) {
		for (int x = 0; x < 4; x++) {
			Vector4f vec = points[x].mul(matrix, temp);
			this.points[x].loadVec3(vec.x, vec.y, vec.z);
		}
	}

	public void loadCameraData(Camera camera, Matrix4f projection) {
		this.projectionMatrix.loadMatrix(projection);
		this.viewMatrix.loadMatrix(camera.getViewMatrixInverse());
		this.cameraPosition.loadVec3(camera.getPosition().getInternal());
		this.inverseProjectionMatrix.loadMatrix(projection.invert(projInv));
		this.inverseViewMatrix.loadMatrix(camera.getViewMatrixInverse().invert(viewInv));
	}
}
