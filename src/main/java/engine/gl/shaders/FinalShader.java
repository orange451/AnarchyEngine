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

import engine.gl.shaders.data.Attribute;
import engine.gl.shaders.data.UniformSampler;

public class FinalShader extends ShaderProgram {

	private UniformSampler image = new UniformSampler("image");

	private String name;

	public FinalShader(String name) {
		this.name = name;
	}

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/" + name + ".vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/" + name + ".fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		super.storeUniforms(image);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		super.stop();
	}

}
