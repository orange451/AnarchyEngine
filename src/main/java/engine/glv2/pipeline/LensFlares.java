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
import engine.glv2.pipeline.shaders.LensFlaresShader;
import engine.glv2.v2.DeferredPass;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.RendererData;
import engine.resources.ResourcesManager;

public class LensFlares extends DeferredPass<LensFlaresShader> {

	private Texture lensColor;

	public LensFlares() {
		super("LensFlares");
	}

	@Override
	public void init(int width, int height) {
		super.init(width, height);
		lensColor = ResourcesManager.loadTextureMisc("assets/textures/lens/lens_color.png", null).get();
	}

	@Override
	protected LensFlaresShader setupShader() {
		return new LensFlaresShader();
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		auxTex[1].active(GL_TEXTURE0);
		lensColor.active(GL_TEXTURE1);
		auxTex[1] = auxTex[0];
	}

	@Override
	public void dispose() {
		super.dispose();
		lensColor.dispose();
	}

}