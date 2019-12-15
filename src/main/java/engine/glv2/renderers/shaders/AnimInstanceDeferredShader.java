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

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import engine.gl.MaterialGL;
import engine.glv2.shaders.ShaderProgram;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformBoolean;
import engine.glv2.shaders.data.UniformMaterial;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.lua.type.object.insts.Camera;

public class AnimInstanceDeferredShader extends ShaderProgram {

	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformMatrix4 jitterMatrix = new UniformMatrix4("jitterMatrix");
	private UniformMaterial material = new UniformMaterial("material");
	private UniformBoolean useTAA = new UniformBoolean("useTAA");

	private UniformMatrix4 transformationMatrixPrev = new UniformMatrix4("transformationMatrixPrev");
	private UniformMatrix4 viewMatrixPrev = new UniformMatrix4("viewMatrixPrev");
	private UniformMatrix4 projectionMatrixPrev = new UniformMatrix4("projectionMatrixPrev");

	private UniformMatrix4 boneMat = new UniformMatrix4("boneMat");
	private UniformMatrix4 boneMatPrev = new UniformMatrix4("boneMatPrev");

	private Matrix4f jitter = new Matrix4f();

	private int frameCont;

	// TODO: Move this
	private Vector2f sampleLocs[] = { new Vector2f(-7.0f, 1.0f).mul(1.0f / 8.0f),
			new Vector2f(-5.0f, -5.0f).mul(1.0f / 8.0f), new Vector2f(-1.0f, -3.0f).mul(1.0f / 8.0f),
			new Vector2f(3.0f, -7.0f).mul(1.0f / 8.0f), new Vector2f(5.0f, -1.0f).mul(1.0f / 8.0f),
			new Vector2f(7.0f, 7.0f).mul(1.0f / 8.0f), new Vector2f(1.0f, 3.0f).mul(1.0f / 8.0f),
			new Vector2f(-3.0f, 5.0f).mul(1.0f / 8.0f) };

	private Vector2f tmp = new Vector2f();

	public AnimInstanceDeferredShader() {
		super("assets/shaders/renderers/AnimInstanceDeferred.vs", "assets/shaders/renderers/InstanceDeferred.fs",
				new Attribute(0, "position"), new Attribute(1, "normals"), new Attribute(2, "textureCoords"),
				new Attribute(3, "inColor"), new Attribute(4, "boneIndices"), new Attribute(5, "boneWeights"));
		super.storeUniforms(transformationMatrix, material, projectionMatrix, viewMatrix, jitterMatrix, useTAA, boneMat,
				transformationMatrixPrev, viewMatrixPrev, projectionMatrixPrev, boneMatPrev);
	}

	public void loadTransformationMatrix(Matrix4f matrix) {
		transformationMatrix.loadMatrix(matrix);
	}

	public void loadTransformationMatrixPrev(Matrix4f matrix) {
		transformationMatrixPrev.loadMatrix(matrix);
	}

	public void loadMaterial(MaterialGL mat) {
		material.loadMaterial(mat);
	}

	public void loadBoneMat(FloatBuffer mat) {
		boneMat.loadMatrix(mat);
	}

	public void loadBoneMatPrev(FloatBuffer mat) {
		boneMatPrev.loadMatrix(mat);
	}

	public void loadCameraPrev(Matrix4f viewMatrixPrev, Matrix4f projectionMatrixPrev) {
		this.viewMatrixPrev.loadMatrix(viewMatrixPrev);
		this.projectionMatrixPrev.loadMatrix(projectionMatrixPrev);
	}

	public void loadCamera(Camera camera, Matrix4f projection, Vector2f resolution, boolean taa) {
		projectionMatrix.loadMatrix(projection);
		viewMatrix.loadMatrix(camera.getViewMatrix().getInternal());
		useTAA.loadBoolean(taa);
		if (taa) {
			Vector2f texSize = new Vector2f(1.0f / resolution.x, 1.0f / resolution.y);

			Vector2f subsampleSize = texSize.mul(2.0f, new Vector2f());

			Vector2f S = sampleLocs[frameCont];

			Vector2f subsample = S.mul(subsampleSize, tmp);
			subsample.mul(0.5f);
			jitter.translation(subsample.x, subsample.y, 0);
			jitterMatrix.loadMatrix(jitter);
			frameCont++;
			frameCont %= 4;
		}
	}
}