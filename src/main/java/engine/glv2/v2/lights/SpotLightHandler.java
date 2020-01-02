/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.v2.lights;

import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.gl.light.SpotLightInternal;
import engine.glv2.objects.Framebuffer;
import engine.glv2.objects.FramebufferBuilder;
import engine.glv2.objects.Texture;
import engine.glv2.objects.TextureBuilder;
import engine.glv2.objects.VAO;
import engine.glv2.shaders.SpotLightShader;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.RenderingSettings;
import engine.glv2.v2.lights.mesh.ConeGenerator;
import engine.lua.type.object.insts.Camera;
import engine.tasks.TaskManager;

public class SpotLightHandler implements ISpotLightHandler {

	private List<SpotLightInternal> lights = new ArrayList<>();

	private VAO cone;

	private Framebuffer main;
	private Texture mainTex;

	private SpotLightShader shader;

	private int width, height;

	private Matrix4f temp = new Matrix4f();
	private Vector2f texel = new Vector2f();

	private static final Vector3f UP = new Vector3f(0, 1, 0);

	public SpotLightHandler(int width, int height) {
		this.width = width;
		this.height = height;
		this.texel.set(1f / (float) width, 1f / (float) height);
		init();
	}

	public void init() {
		shader = new SpotLightShader();
		shader.init();
		cone = ConeGenerator.create(32);
		generateFramebuffer();
	}

	public void render(Camera camera, Matrix4f projectionMatrix, DeferredPipeline dp, RenderingSettings rs) {
		main.bind();
		glCullFace(GL_FRONT);
		glClear(GL_COLOR_BUFFER_BIT);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE);
		shader.start();
		shader.loadCameraData(camera, projectionMatrix);
		shader.loadUseShadows(rs.shadowsEnabled);
		shader.loadTexel(texel);
		activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, dp.getDiffuseTex().getTexture());
		activateTexture(GL_TEXTURE2, GL_TEXTURE_2D, dp.getNormalTex().getTexture());
		activateTexture(GL_TEXTURE3, GL_TEXTURE_2D, dp.getDepthTex().getTexture());
		activateTexture(GL_TEXTURE4, GL_TEXTURE_2D, dp.getPbrTex().getTexture());
		activateTexture(GL_TEXTURE5, GL_TEXTURE_2D, dp.getMaskTex().getTexture());
		cone.bind(0);
		for (SpotLightInternal l : lights) {
			if (!l.visible)
				continue;
			temp.identity();
			temp.translate(l.position);
			temp.rotateTowards(l.direction, UP);
			temp.scaleAround(1.1f, 0.0f, 0f, 0.5f);
			float fov = (float) Math.tan(Math.toRadians(l.outerFOV * 0.5f)) * l.radius;
			temp.scale(fov, fov, l.radius);
			shader.loadTransformationMatrix(temp);
			shader.loadSpotLight(l);
			activateTexture(GL_TEXTURE6, GL_TEXTURE_2D, l.getShadowMap().getShadowMap().getTexture());
			glDrawElements(GL_TRIANGLES, cone.getIndexCount(), GL_UNSIGNED_INT, 0);
		}
		cone.unbind(0);
		shader.stop();
		glCullFace(GL_BACK);
		glDisable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		main.unbind();
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		this.texel.set(1f / (float) width, 1f / (float) height);
		disposeFramebuffer();
		generateFramebuffer();
	}

	public void dispose() {
		disposeFramebuffer();
		cone.dispose();
	}

	private void activateTexture(int textureNum, int target, int texture) {
		glActiveTexture(textureNum);
		glBindTexture(target, texture);
	}

	private void generateFramebuffer() {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
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
	public void addLight(SpotLightInternal l) {
		if (l == null)
			return;
		TaskManager.addTaskRenderThread(() -> {
			l.init();
			lights.add(l);
		});
	}

	@Override
	public void removeLight(SpotLightInternal l) {
		if (l == null)
			return;
		TaskManager.addTaskRenderThread(() -> {
			lights.remove(l);
			l.dispose();
		});
	}

	public List<SpotLightInternal> getLights() {
		return lights;
	}

}
