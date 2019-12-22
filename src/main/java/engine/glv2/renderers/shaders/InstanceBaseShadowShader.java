/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.renderers.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;

import org.joml.Matrix4f;

import engine.glv2.shaders.ShaderProgram;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformFloat;
import engine.glv2.shaders.data.UniformMatrix4;

public abstract class InstanceBaseShadowShader extends ShaderProgram {

	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	protected UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformFloat transparency = new UniformFloat("transparency");

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/renderers/InstanceShadow.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		super.storeUniforms(transformationMatrix, viewMatrix, transparency);
	}

	public void loadTransformationMatrix(Matrix4f matrix) {
		transformationMatrix.loadMatrix(matrix);
	}

	public void loadTransparency(float t) {
		transparency.loadFloat(t);
	}

}
