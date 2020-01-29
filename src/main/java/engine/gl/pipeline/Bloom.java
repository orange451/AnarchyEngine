/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.pipeline;

import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;

import engine.gl.DeferredPass;
import engine.gl.DeferredPipeline;
import engine.gl.RendererData;
import engine.gl.objects.Texture;
import engine.gl.pipeline.shaders.BloomShader;

public class Bloom extends DeferredPass<BloomShader> {

	public Bloom() {
		super("Bloom");
	}

	@Override
	protected BloomShader setupShader() {
		return new BloomShader();
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		auxTex[0].active(GL_TEXTURE0);
		auxTex[1].active(GL_TEXTURE1);
		auxTex[1] = auxTex[0];
	}

}