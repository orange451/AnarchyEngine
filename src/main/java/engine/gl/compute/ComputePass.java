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

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;

import org.joml.Vector2f;

import engine.gl.GPUProfiler;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;

public abstract class ComputePass<T extends BaseComputeShader> {

	private int width, height;
	protected Texture mainTex;

	private T shader;

	protected String name;

	private float scaling = 1.0f;

	private int frameCont;

	public ComputePass(String name) {
		this.name = name;
	}

	public ComputePass(String name, float scaling) {
		this(name);
		this.scaling = scaling;
	}

	public void init(int iWidth, int iHeight) {
		width = (int) (iWidth * scaling);
		height = (int) (iHeight * scaling);
		generateFramebuffer(width, height);
		shader = setupShader();
		shader.initCompute();
		shader.start();
		shader.loadResolution(new Vector2f(width, height));
		shaderResizeSetup(width, height, shader);
		shader.stop();
	}

	public void process(RendererData rnd, IRenderingData rd, ComputePipeline pl, Texture[] auxTextures) {
		frameCont += 1;
		frameCont %= Integer.MAX_VALUE;
		GPUProfiler.start(name);
		setupImage();
		shader.start();
		shader.loadSettings(rnd.rs);
		shader.loadFrame(frameCont);
		setupShaderData(rnd, rd, shader);
		setupTextures(rnd, pl, auxTextures);
		dispatch(width, height, shader);
		shader.stop();
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

	protected void setupImage() {
	}

	protected void setupShaderData(RendererData rnd, IRenderingData rd, T shader) {
	}

	protected void shaderResizeSetup(int width, int height, T shader) {
	}

	protected void dispatch(int width, int height, T shader) {
		shader.dispatch(width / 16, height / 16, 1);
	}

	protected abstract void setupTextures(RendererData rnd, ComputePipeline dp, Texture[] auxTex);

	public void resize(int iWidth, int iHeight) {
		width = (int) (iWidth * scaling);
		height = (int) (iHeight * scaling);
		disposeFramebuffer();
		generateFramebuffer(width, height);
		shader.start();
		shader.loadResolution(new Vector2f(width, height));
		shaderResizeSetup(width, height, shader);
		shader.stop();
	}

	public void dispose() {
		disposeFramebuffer();
		shader.dispose();
	}

	public void reloadShader() {
		shader.reloadCompute();
		shader.start();
		shader.loadResolution(new Vector2f(width, height));
		shaderResizeSetup(width, height, shader);
		shader.stop();
	}

	protected void generateFramebuffer(int width, int height) {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		mainTex = tb.endTexture();
	}

	protected void disposeFramebuffer() {
		mainTex.dispose();
	}

}
