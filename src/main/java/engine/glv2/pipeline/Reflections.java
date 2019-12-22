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

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE7;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;

import engine.glv2.objects.Texture;
import engine.glv2.pipeline.shaders.ReflectionsShader;
import engine.glv2.v2.DeferredPass;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.IRenderingData;
import engine.glv2.v2.RendererData;

public class Reflections extends DeferredPass<ReflectionsShader> {

	public Reflections() {
		super("Reflections");
	}

	@Override
	protected ReflectionsShader setupShader() {
		return new ReflectionsShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, ReflectionsShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		super.activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, dp.getDiffuseTex().getTexture());
		super.activateTexture(GL_TEXTURE1, GL_TEXTURE_2D, dp.getNormalTex().getTexture());
		super.activateTexture(GL_TEXTURE2, GL_TEXTURE_2D, dp.getDepthTex().getTexture());
		super.activateTexture(GL_TEXTURE3, GL_TEXTURE_2D, dp.getPbrTex().getTexture());
		super.activateTexture(GL_TEXTURE4, GL_TEXTURE_2D, dp.getMaskTex().getTexture());
		super.activateTexture(GL_TEXTURE5, GL_TEXTURE_CUBE_MAP, rnd.environmentMap.getTexture());
		super.activateTexture(GL_TEXTURE6, GL_TEXTURE_2D, rnd.brdfLUT.getTexture());
		super.activateTexture(GL_TEXTURE7, GL_TEXTURE_2D, auxTex[0].getTexture());
		super.activateTexture(GL_TEXTURE8, GL_TEXTURE_2D, auxTex[1].getTexture());
	}

}
