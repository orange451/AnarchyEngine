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
import engine.gl.pipeline.shaders.LensFlareModShader;
import engine.resources.ResourcesManager;

public class LensFlareMod extends DeferredPass<LensFlareModShader> {

	private Texture lensDirt;
	private Texture lensStar;

	public LensFlareMod() {
		super("LensFlaresMod");
	}

	@Override
	public void init(int width, int height) {
		super.init(width, height);
		lensDirt = ResourcesManager.loadTextureMisc("assets/textures/lens/lens_dirt.png", null).get();
		lensStar = ResourcesManager.loadTextureMisc("assets/textures/lens/lens_star.png", null).get();
	}

	@Override
	protected LensFlareModShader setupShader() {
		return new LensFlareModShader();
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		auxTex[0].active(GL_TEXTURE0);
		lensDirt.active(GL_TEXTURE1);
		lensStar.active(GL_TEXTURE2);
		auxTex[1].active(GL_TEXTURE3);
	}

	@Override
	public void dispose() {
		super.dispose();
		lensDirt.dispose();
		lensStar.dispose();
	}

}