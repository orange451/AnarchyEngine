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
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;

import engine.glv2.objects.Texture;
import engine.glv2.pipeline.shaders.DepthOfFieldShader;
import engine.glv2.v2.PostProcesPass;
import engine.glv2.v2.PostProcessPipeline;
import engine.glv2.v2.RendererData;

public class DepthOfField extends PostProcesPass<DepthOfFieldShader> {

	public DepthOfField() {
		super("DoF");
	}

	@Override
	protected DepthOfFieldShader setupShader() {
		return new DepthOfFieldShader();
	}

	@Override
	protected void setupTextures(RendererData rnd, PostProcessPipeline pp, Texture[] auxTex) {
		auxTex[0].active(GL_TEXTURE0);
		pp.getDepthTex().active(GL_TEXTURE1);
	}

}
