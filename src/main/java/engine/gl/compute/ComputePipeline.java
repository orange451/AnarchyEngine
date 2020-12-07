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
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT2;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT3;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT4;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_COMPONENT32F;
import static org.lwjgl.opengl.GL30C.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_RG;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;
import static org.lwjgl.opengl.GL30C.glBindFramebuffer;
import static org.lwjgl.opengl.GL30C.glBlitFramebuffer;
import static org.lwjgl.opengl.GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import engine.gl.GPUProfiler;
import engine.gl.IDeferredPipeline;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.objects.Framebuffer;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;
import engine.gl.objects.VAO;
import engine.gl.shaders.FinalShader;

public abstract class ComputePipeline implements IDeferredPipeline {

	protected int width, height;
	private Texture[] auxTex;

	public enum PipelineSteps {
		AMBIENT_OCCLUSION, AO_BLUR, LIGHTING, MOTION_BLUR, COLOR_CORRECTION, ANTI_ALIASING
	}

	private Map<PipelineSteps, ComputePass<?>> steps;

	private Framebuffer main;
	private Texture diffuseTex, motionTex, normalTex, pbrTex, maskTex, depthTex;

	private Framebuffer previousFrame;
	private Texture previousFrameTex;

	// Temporary
	private VAO quad;
	private FinalShader finalShader;

	public ComputePipeline(int width, int height) {
		this.width = width;
		this.height = height;
		steps = new EnumMap<>(PipelineSteps.class);
		auxTex = new Texture[3];
		init();
	}

	public void init() {
		generatePipeline();
		setupPasses();
		for (Entry<PipelineSteps, ComputePass<?>> pass : steps.entrySet())
			pass.getValue().init(width, height);

		// Remove this
		finalShader = new FinalShader("deferred/Final");
		finalShader.init();
		float[] positions = { -1, 1, -1, -1, 1, 1, 1, -1 };
		quad = VAO.create();
		quad.bind();
		quad.createAttribute(0, positions, 2, GL_STATIC_DRAW);
		quad.unbind();
		quad.setVertexCount(4);
	}

	public abstract void setupPasses();

	@Override
	public void bind() {
		main.bind();
	}

	@Override
	public void unbind() {
		main.unbind();
	}

	@Override
	public void process(RendererData rnd, IRenderingData rd) {
		for (Entry<PipelineSteps, ComputePass<?>> pass : steps.entrySet())
			pass.getValue().process(rnd, rd, this, auxTex);
	}

	@Override
	public void render(Framebuffer fb) {
		// Wait for compute shaders
		glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

		previousFrame.bind();
		finalShader.start();
		quad.bind();
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, auxTex[0].getTexture());
		glDrawArrays(GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
		quad.unbind();
		finalShader.stop();
		previousFrame.unbind();
		// Blit depth
		GPUProfiler.start("blit");
		glBindFramebuffer(GL_READ_FRAMEBUFFER, main.getFramebuffer());
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fb.getFramebuffer());
		glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
		// Blit color
		glBindFramebuffer(GL_READ_FRAMEBUFFER, previousFrame.getFramebuffer());
		glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
		GPUProfiler.end();
	}

	@Override
	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		disposePipeline();
		generatePipeline();
		for (Entry<PipelineSteps, ComputePass<?>> pass : steps.entrySet())
			pass.getValue().resize(width, height);
	}

	@Override
	public void dispose() {
		disposePipeline();
		for (Entry<PipelineSteps, ComputePass<?>> pass : steps.entrySet())
			pass.getValue().dispose();
		quad.dispose();
		finalShader.dispose();
	}

	@Override
	public void reloadShaders() {
		for (Entry<PipelineSteps, ComputePass<?>> pass : steps.entrySet())
			pass.getValue().reloadShader();
		finalShader.reload();
	}

	public void addStep(PipelineSteps step, ComputePass<?> pass) {
		ComputePass<?> old = steps.put(step, pass);
		if (old != null) {
			old.dispose();
		}
	}

	private void generatePipeline() {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGB16F, 0, GL_RGB, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		diffuseTex = tb.endTexture();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		motionTex = tb.endTexture();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		normalTex = tb.endTexture();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RG, 0, GL_RG, GL_UNSIGNED_BYTE, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		pbrTex = tb.endTexture();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		maskTex = tb.endTexture();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_DEPTH_COMPONENT32F, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		depthTex = tb.endTexture();

		FramebufferBuilder fb = new FramebufferBuilder();

		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(width, height);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, diffuseTex, 0);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT1, motionTex, 0);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT2, normalTex, 0);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT3, pbrTex, 0);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT4, maskTex, 0);
		fb.framebufferTexture(GL_DEPTH_ATTACHMENT, depthTex, 0);
		int bufs[] = { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3,
				GL_COLOR_ATTACHMENT4 };
		fb.drawBuffers(bufs);
		main = fb.endFramebuffer();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		previousFrameTex = tb.endTexture();

		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(width, height);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, previousFrameTex, 0);
		previousFrame = fb.endFramebuffer();
	}

	private void disposePipeline() {
		main.dispose();
		diffuseTex.dispose();
		motionTex.dispose();
		normalTex.dispose();
		pbrTex.dispose();
		maskTex.dispose();
		depthTex.dispose();
		previousFrame.dispose();
		previousFrameTex.dispose();
	}

	@Override
	public Texture getDiffuseTex() {
		return diffuseTex;
	}

	@Override
	public Texture getMotionTex() {
		return motionTex;
	}

	@Override
	public Texture getNormalTex() {
		return normalTex;
	}

	@Override
	public Texture getPbrTex() {
		return pbrTex;
	}

	@Override
	public Texture getMaskTex() {
		return maskTex;
	}

	@Override
	public Texture getDepthTex() {
		return depthTex;
	}

	public Texture getPreviousFrameTex() {
		return previousFrameTex;
	}

}
