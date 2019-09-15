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

import engine.glv2.entities.SunCamera;
import engine.glv2.shaders.ShaderProgram;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformFloat;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.lua.type.object.insts.Camera;

public class AnimInstanceBasicShader extends ShaderProgram {

	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	private UniformMatrix4 projectionMatrix[] = new UniformMatrix4[4];
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformFloat transparency = new UniformFloat("transparency");

	private UniformMatrix4 boneMat = new UniformMatrix4("boneMat");

	public AnimInstanceBasicShader() {
		super("assets/shaders/renderers/AnimInstanceBasic.vs", "assets/shaders/renderers/InstanceBasic.gs",
				"assets/shaders/renderers/InstanceBasic.fs", new Attribute(0, "position"), new Attribute(1, "normals"),
				new Attribute(2, "textureCoords"), new Attribute(3, "inColor"), new Attribute(4, "boneIndices"),
				new Attribute(5, "boneWeights"));
		for (int i = 0; i < 4; i++)
			projectionMatrix[i] = new UniformMatrix4("projectionMatrix[" + i + "]");
		super.storeUniforms(projectionMatrix);
		super.storeUniforms(transformationMatrix, viewMatrix, transparency, boneMat);
		super.validate();
	}

	public void loadTransformationMatrix(Matrix4f matrix) {
		transformationMatrix.loadMatrix(matrix);
	}

	public void loadviewMatrix(Camera camera) {
		viewMatrix.loadMatrix(camera.getViewMatrix().getInternal());
	}

	public void loadsunCamera(SunCamera camera) {
		viewMatrix.loadMatrix(camera.getViewMatrix());
		for (int i = 0; i < 4; i++)
			projectionMatrix[i].loadMatrix(camera.getProjectionArray()[i]);
	}

	public void loadTransparency(float t) {
		transparency.loadFloat(t);
	}

	public void loadBoneMat(FloatBuffer mat) {
		boneMat.loadMatrix(mat);
	}

}
