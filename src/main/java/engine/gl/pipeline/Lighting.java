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

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE10;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE11;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE7;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE9;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;

import engine.gl.DeferredPass;
import engine.gl.DeferredPipeline;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;
import engine.gl.pipeline.shaders.LightingShader;

public class Lighting extends DeferredPass<LightingShader> {

	private Texture auxTex;

	public Lighting() {
		super("Lighting");
	}

	@Override
	protected LightingShader setupShader() {
		return new LightingShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, LightingShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		dp.getDiffuseTex().active(GL_TEXTURE0);
		dp.getNormalTex().active(GL_TEXTURE1);
		dp.getDepthTex().active(GL_TEXTURE2);
		dp.getPbrTex().active(GL_TEXTURE3);
		dp.getMaskTex().active(GL_TEXTURE4);
		rnd.irradianceCapture.active(GL_TEXTURE5);
		rnd.environmentMap.active(GL_TEXTURE6);
		rnd.brdfLUT.active(GL_TEXTURE7);
		rnd.dlh.getMainTex().active(GL_TEXTURE8);
		rnd.plh.getMainTex().active(GL_TEXTURE9);
		rnd.slh.getMainTex().active(GL_TEXTURE10);
		rnd.alh.getMainTex().active(GL_TEXTURE11);
	}

	@Override
	protected void setupAuxTextures(Texture[] auxTextures) {
		super.setupAuxTextures(auxTextures);
		auxTextures[1] = auxTex;
	}

	@Override
	protected void generateFramebuffer(int width, int height) {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		mainTex = tb.endTexture();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		auxTex = tb.endTexture();

		FramebufferBuilder fb = new FramebufferBuilder();

		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(width, height);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, mainTex, 0);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT1, auxTex, 0);
		fb.drawBuffers(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1);
		mainBuf = fb.endFramebuffer();
	}

	@Override
	protected void disposeFramebuffer() {
		super.disposeFramebuffer();
		auxTex.dispose();
	}

	@Override
	public void dispose() {
		super.dispose();
	}

}
