/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.lights;

import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import engine.gl.DeferredPipeline;
import engine.gl.RenderingSettings;
import engine.gl.objects.Framebuffer;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;
import engine.gl.objects.VAO;
import engine.gl.shaders.DirectionalLightShader;
import engine.lua.type.object.insts.Camera;
import engine.tasks.TaskManager;

public class DirectionalLightHandler implements ILightHandler<DirectionalLightInternal> {

	private List<DirectionalLightInternal> lights = new ArrayList<>();

	private VAO quad;

	private Framebuffer main;
	private Texture mainTex;

	private DirectionalLightShader shader;

	private int width, height;

	public DirectionalLightHandler(int width, int height) {
		this.width = width;
		this.height = height;
		init();
	}

	public void init() {
		float[] positions = { -1, 1, -1, -1, 1, 1, 1, -1 };
		quad = VAO.create();
		quad.bind();
		quad.createAttribute(0, positions, 2, GL_STATIC_DRAW);
		quad.unbind();
		quad.setVertexCount(4);
		shader = new DirectionalLightShader();
		shader.init();
		generateFramebuffer();
	}

	public void render(Camera camera, Matrix4f projectionMatrix, DeferredPipeline dp, RenderingSettings rs) {
		main.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE);
		shader.start();
		shader.loadCameraData(camera, projectionMatrix);
		shader.loadUseShadows(rs.shadowsEnabled);
		quad.bind();
		dp.getDiffuseTex().active(GL_TEXTURE0);
		dp.getNormalTex().active(GL_TEXTURE2);
		dp.getDepthTex().active(GL_TEXTURE3);
		dp.getPbrTex().active(GL_TEXTURE4);
		dp.getMaskTex().active(GL_TEXTURE5);
		for (DirectionalLightInternal l : lights) {
			if (!l.visible)
				continue;
			shader.loadDirectionalLight(l);
			l.getShadowMap().getTexture().active(GL_TEXTURE6);
			glDrawArrays(GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
		}
		quad.unbind();
		shader.stop();
		glDisable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		main.unbind();
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		disposeFramebuffer();
		generateFramebuffer();
	}

	public void dispose() {
		disposeFramebuffer();
		quad.dispose();
	}

	private void generateFramebuffer() {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGB16F, 0, GL_RGB, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		mainTex = tb.endTexture();
		FramebufferBuilder fb = new FramebufferBuilder();

		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(width, height);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, mainTex, 0);
		main = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		main.dispose();
		mainTex.dispose();
	}

	public Texture getMainTex() {
		return mainTex;
	}

	@Override
	public void addLight(DirectionalLightInternal l) {
		if (l == null)
			return;
		TaskManager.addTaskRenderThread(() -> {
			l.init();
			lights.add(l);
		});
	}

	@Override
	public void removeLight(DirectionalLightInternal l) {
		if (l == null)
			return;
		TaskManager.addTaskRenderThread(() -> {
			lights.remove(l);
			l.dispose();
		});
	}

	public List<DirectionalLightInternal> getLights() {
		return lights;
	}

}
