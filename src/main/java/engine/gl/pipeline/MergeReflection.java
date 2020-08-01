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
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;

import engine.gl.DeferredPass;
import engine.gl.DeferredPipeline;
import engine.gl.RendererData;
import engine.gl.objects.Texture;
import engine.gl.pipeline.shaders.MergeReflectionShader;

public class MergeReflection extends DeferredPass<MergeReflectionShader> {

	public MergeReflection() {
		super("MergeReflection");
	}

	@Override
	protected MergeReflectionShader setupShader() {
		return new MergeReflectionShader();
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		dp.getMaskTex().active(GL_TEXTURE0);
		auxTex[1].active(GL_TEXTURE1);
		auxTex[0].active(GL_TEXTURE2);
		auxTex[2].active(GL_TEXTURE3);
	}

}
