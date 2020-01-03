/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.pipeline;

import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import engine.glv2.objects.Texture;
import engine.glv2.pipeline.shaders.BasicPostProcessShader;
import engine.glv2.v2.PostProcesPass;
import engine.glv2.v2.PostProcessPipeline;
import engine.glv2.v2.RendererData;

public class FXAA extends PostProcesPass<BasicPostProcessShader> {

	public FXAA() {
		super("FXAA");
	}

	@Override
	protected BasicPostProcessShader setupShader() {
		return new BasicPostProcessShader(name) {
			@Override
			protected void setupShader() {
				super.setupShader();
				super.addShader(new Shader("assets/shaders/postprocess/" + name + ".vs", GL_VERTEX_SHADER));
			}
		};
	}

	@Override
	protected void setupTextures(RendererData rnd, PostProcessPipeline pp, Texture[] auxTex) {
		auxTex[0].active(GL_TEXTURE0);
	}

}
