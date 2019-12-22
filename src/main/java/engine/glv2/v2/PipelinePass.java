/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.v2;

import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

import org.joml.Vector2f;

import engine.glv2.objects.Framebuffer;
import engine.glv2.objects.Texture;
import engine.glv2.objects.VAO;
import engine.glv2.pipeline.shaders.BasePipelineShader;

public abstract class PipelinePass<T extends BasePipelineShader, P> {

	private int width, height;
	protected Framebuffer mainBuf;
	protected Texture mainTex;

	private T shader;

	protected String name;

	private float scaling = 1.0f;

	private int frameCont;

	public PipelinePass(String name) {
		this.name = name;
	}

	public PipelinePass(String name, float scaling) {
		this(name);
		this.scaling = scaling;
	}

	public void init(int iWidth, int iHeight) {
		width = (int) (iWidth * scaling);
		height = (int) (iHeight * scaling);
		generateFramebuffer(width, height);
		shader = setupShader();
		shader.init();
		shader.start();
		shader.loadResolution(new Vector2f(width, height));
		shader.stop();
	}

	public void process(RendererData rnd, IRenderingData rd, P pl, Texture[] auxTextures, VAO quad) {
		frameCont += 1;
		frameCont %= Integer.MAX_VALUE;
		GPUProfiler.start(name);
		mainBuf.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		shader.start();
		shader.loadSettings(rnd.rs);
		shader.loadFrame(frameCont);
		setupShaderData(rnd, rd, shader);
		setupTextures(rnd, pl, auxTextures);
		glDrawArrays(GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
		shader.stop();
		mainBuf.unbind();
		GPUProfiler.end();
		this.setupAuxTextures(auxTextures);
	}

	/**
	 * 
	 * Setups the output aux texture slots used as input for the next pass, by
	 * default sets the [0] slot as the current pass texture
	 * 
	 * @param auxTextures Array of auxiliary texture slots
	 */
	protected void setupAuxTextures(Texture[] auxTextures) {
		auxTextures[0] = mainTex;
	}

	protected abstract T setupShader();

	protected void setupShaderData(RendererData rnd, IRenderingData rd, T shader) {
	}

	protected void activateTexture(int textureNum, int target, int texture) {
		glActiveTexture(textureNum);
		glBindTexture(target, texture);
	}

	protected void activateTexture(int textureNum, Texture texture) {
		glActiveTexture(textureNum);
		texture.bind();
	}

	protected abstract void setupTextures(RendererData rnd, P dp, Texture[] auxTex);

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

	protected abstract void generateFramebuffer(int width, int height);

	protected void disposeFramebuffer() {
		mainBuf.dispose();
		mainTex.dispose();
	}

}
