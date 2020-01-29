/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl;

import static org.lwjgl.nanovg.NanoVG.NVG_IMAGE_FLIPY;
import static org.lwjgl.nanovg.NanoVG.nvgDeleteImage;
import static org.lwjgl.nanovg.NanoVGGL3.nvglCreateImageFromHandle;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_COMPONENT32F;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2i;

import engine.ClientEngine;
import engine.gl.objects.Framebuffer;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;
import engine.gl.objects.VAO;
import engine.gl.shaders.FinalShader;
import lwjgui.scene.Context;
import lwjgui.scene.layout.StackPane;
import lwjgui.style.BackgroundNVGImage;

public abstract class PostProcessPipeline {

	protected int width, height;
	private Texture[] auxTex;

	protected List<PostProcesPass<?>> passes;

	private Framebuffer main;
	private Texture mainTex, depthTex;

	private VAO quad;

	private FinalShader finalShader;

	private int nvgTexture;

	private long nvg;

	private StackPane displayPane;
	private BackgroundNVGImage image;

	public PostProcessPipeline(int width, int height, long nvg) {
		this.width = width;
		this.height = height;
		this.nvg = nvg;
		passes = new ArrayList<>();
		auxTex = new Texture[3];
		init();
	}

	public void init() {
		float[] positions = { -1, 1, -1, -1, 1, 1, 1, -1 };
		quad = VAO.create();
		quad.bind();
		quad.createAttribute(0, positions, 2, GL_STATIC_DRAW);
		quad.unbind();
		quad.setVertexCount(4);
		generatePipeline();
		setupPasses();
		for (PostProcesPass<?> pass : passes)
			pass.init(width, height);
		finalShader = new FinalShader("postprocess/Final");
		finalShader.init();
		displayPane = new StackPane() {
			private Vector2i size = new Vector2i();
			private boolean needsResize;

			@Override
			protected void resize() {
				super.resize();
				int width = (int) getWidth();
				int height = (int) getHeight();
				if (!needsResize)
					needsResize = !size.equals(width, height);
				size.set(width, height);
			}

			@Override
			public void render(Context context) {
				super.render(context);
				if (!needsResize)
					ClientEngine.renderThread.resizedPipeline(size.x, size.y);
				needsResize = false;
			}
		};
		displayPane.setBackground(image = new BackgroundNVGImage(nvgTexture));
		displayPane.setMinSize(2, 2);
		displayPane.setFillToParentHeight(true);
		displayPane.setFillToParentWidth(true);
	}

	public abstract void setupPasses();

	public void bind() {
		main.bind();
	}

	public void unbind() {
		main.unbind();
	}

	public void process(RendererData rnd, IRenderingData rd) {
		glDisable(GL_DEPTH_TEST);
		quad.bind(0);
		for (PostProcesPass<?> pass : passes)
			pass.process(rnd, rd, this, auxTex, quad);
		quad.unbind(0);
		glEnable(GL_DEPTH_TEST);
	}

	public void render() {
		glDisable(GL_CULL_FACE);
		glDisable(GL_DEPTH_TEST);
		main.bind();
		finalShader.start();
		quad.bind(0);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, auxTex[0].getTexture());
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		quad.unbind(0);
		finalShader.stop();
		main.unbind();
		glEnable(GL_DEPTH_TEST);
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		disposePipeline();
		generatePipeline();
		for (PostProcesPass<?> pass : passes)
			pass.resize(width, height);
		image.setNVGImage(nvgTexture);
	}

	public void dispose() {
		disposePipeline();
		for (PostProcesPass<?> pass : passes)
			pass.dispose();
		quad.dispose();
		finalShader.dispose();
	}

	public void reloadShaders() {
		for (PostProcesPass<?> deferredPass : passes)
			deferredPass.reloadShader();
		finalShader.reload();
	}

	private void generatePipeline() {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		mainTex = tb.endTexture();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_DEPTH_COMPONENT32F, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		depthTex = tb.endTexture();

		FramebufferBuilder fb = new FramebufferBuilder();

		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(width, height);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, mainTex, 0);
		fb.framebufferTexture(GL_DEPTH_ATTACHMENT, depthTex, 0);
		main = fb.endFramebuffer();

		nvgTexture = nvglCreateImageFromHandle(nvg, mainTex.getTexture(), width, width, NVG_IMAGE_FLIPY);
	}

	private void disposePipeline() {
		main.dispose();
		mainTex.dispose();
		depthTex.dispose();
		nvgDeleteImage(nvg, nvgTexture);
	}

	public Texture getMainTex() {
		return mainTex;
	}

	public Texture getDepthTex() {
		return depthTex;
	}

	public StackPane getDisplayPane() {
		return displayPane;
	}

	/**
	 * <b>INTERAL USE ONLY</b><br>
	 * Used for blitting the depth buffer of the
	 * {@link net.luxvacuos.lightengine.client.rendering.opengl.v2.DeferredPipeline
	 * DeferredPipeline} to the
	 * {@link net.luxvacuos.lightengine.client.rendering.opengl.v2.PostProcessPipeline
	 * PostProcessPipeline}
	 **/
	public Framebuffer getMain() {
		return main;
	}

}
