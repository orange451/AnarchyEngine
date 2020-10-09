/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.compute;

import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE10;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE11;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE12;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE9;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL46C.*;

import org.joml.Vector2f;

import engine.gl.DeferredPipeline;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.objects.Framebuffer;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;
import engine.gl.shaders.ComputeAOShader;

public class ComputeAO {

	private int width, height;
	protected Texture mainTex;

	private ComputeAOShader shader;

	protected String name;
	
	private float scaling = 1.0f;

	private int frameCont;

	public ComputeAO(int iWidth, int iHeight) {
		width = (int) (iWidth * scaling);
		height = (int) (iHeight * scaling);
		generateFramebuffer(width, height);
		shader = new ComputeAOShader();
		shader.initCompute();
		shader.start();
		shader.loadResolution(new Vector2f(width, height));
		shader.stop();
	}

	public void process(RendererData rnd, IRenderingData rd, DeferredPipeline dp) {

		frameCont += 1;
		frameCont %= Integer.MAX_VALUE;
		//GPUProfiler.start(name);
		shader.start();
		glBindImageTexture(0, mainTex.getTexture(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA16F);
		dp.getNormalTex().active(GL_TEXTURE0);
		dp.getDepthTex().active(GL_TEXTURE1);
		dp.getMaskTex().active(GL_TEXTURE2);
		dp.getMotionTex().active(GL_TEXTURE3);
		rnd.dlh.getMainTex().active(GL_TEXTURE4);
		rnd.plh.getMainTex().active(GL_TEXTURE5);
		rnd.slh.getMainTex().active(GL_TEXTURE6);
		rnd.alh.getMainTex().active(GL_TEXTURE7);
		rnd.vm.getColor().active(GL_TEXTURE8);
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
		shader.loadVoxelSize(rnd.vm.getSize() * 2.0f);
		shader.loadVoxelOffset(rnd.vm.getCameraOffset());
		shader.loadFrame(frameCont);
		glDispatchCompute(width / 16, height / 16, 1);
		shader.stop();
		//GPUProfiler.end();
	}

	public void resize(int iWidth, int iHeight) {
		width = (int) (iWidth * scaling);
		height = (int) (iHeight * scaling);
		disposeFramebuffer();
		generateFramebuffer(width, height);
		shader.start();
		shader.loadResolution(new Vector2f(width, height));
		shader.stop();
	}

	public void dispose() {
		disposeFramebuffer();
		shader.dispose();
	}

	public void reloadShader() {
		shader.reload();
		shader.start();
		shader.loadResolution(new Vector2f(width, height));
		shader.stop();
	}
	
	public Texture getTexture() {
		return mainTex;
	}

	private void generateFramebuffer(int width, int height) {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		mainTex = tb.endTexture();
	}

	private void disposeFramebuffer() {
		mainTex.dispose();
	}

}
