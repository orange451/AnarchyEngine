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

import static org.lwjgl.opengl.GL11C.GL_BACK;
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
import static org.lwjgl.opengl.GL11C.glCullFace;
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
import static org.lwjgl.opengl.GL13C.GL_TEXTURE7;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import engine.gl.DeferredPipeline;
import engine.gl.RenderingSettings;
import engine.gl.objects.Framebuffer;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;
import engine.gl.objects.VAO;
import engine.gl.shaders.AreaLightShader;
import engine.lua.type.object.insts.Camera;
import engine.resources.ResourcesManager;
import engine.tasks.Task;
import engine.tasks.TaskManager;

public class AreaLightHandler implements ILightHandler<AreaLightInternal> {

	private List<AreaLightInternal> lights = new ArrayList<>();

	private VAO quad;

	private Framebuffer main;
	private Texture mainTex;

	private AreaLightShader shader;

	private int width, height;

	private Matrix4f temp = new Matrix4f();

	private Texture ltcMag, ltcMat;

	private static final Vector3f UP = new Vector3f(0, 1, 0);

	private static final Vector4f[] POINTS = { new Vector4f(-0.5f, -0.5f, 0, 1), new Vector4f(-0.5f, 0.5f, 0, 1),
			new Vector4f(0.5f, 0.5f, 0, 1), new Vector4f(0.5f, -0.5f, 0, 1) };

	public AreaLightHandler(int width, int height) {
		this.width = width;
		this.height = height;
		Game.userInputService().inputBeganEvent().connect((args) -> {
			if (args[0].get("KeyCode").eq_b(LuaValue.valueOf(GLFW.GLFW_KEY_F6))) {
				shader.reload();
			}
		});
		init();
	}

	public void init() {
		Task<Texture> ltcMagTask = ResourcesManager.loadTextureMisc("assets/textures/ltc_mag.png", null);
		Task<Texture> ltcMatTask = ResourcesManager.loadTextureMisc("assets/textures/ltc_mat.png", null);
		float[] positions = { -1, 1, -1, -1, 1, 1, 1, -1 };
		quad = VAO.create();
		quad.bind();
		quad.createAttribute(0, positions, 2, GL_STATIC_DRAW);
		quad.unbind();
		quad.setVertexCount(4);
		shader = new AreaLightShader();
		shader.init();
		generateFramebuffer();
		ltcMag = ltcMagTask.get();
		ltcMat = ltcMatTask.get();
	}

	public void render(Camera camera, Matrix4f projectionMatrix, DeferredPipeline dp, RenderingSettings rs) {
		main.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE);
		shader.start();
		shader.loadCameraData(camera, projectionMatrix);
		dp.getDiffuseTex().active(GL_TEXTURE0);
		dp.getNormalTex().active(GL_TEXTURE2);
		dp.getDepthTex().active(GL_TEXTURE3);
		dp.getPbrTex().active(GL_TEXTURE4);
		dp.getMaskTex().active(GL_TEXTURE5);
		ltcMag.active(GL_TEXTURE6);
		ltcMat.active(GL_TEXTURE7);
		quad.bind(0);
		for (AreaLightInternal l : lights) {
			if (!l.visible)
				continue;
			temp.identity();
			temp.translate(l.position);
			temp.rotateTowards(l.direction, UP);
			temp.scale(l.sizeY, l.sizeX, 1.0f);
			shader.loadPoints(POINTS, temp);
			shader.loadAreaLight(l);
			glDrawArrays(GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
		}
		quad.unbind(0);
		shader.stop();
		glCullFace(GL_BACK);
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
		ltcMag.dispose();
		ltcMat.dispose();
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
	public void addLight(AreaLightInternal l) {
		if (l == null)
			return;
		TaskManager.addTaskRenderThread(() -> {
			lights.add(l);
		});
	}

	@Override
	public void removeLight(AreaLightInternal l) {
		if (l == null)
			return;
		TaskManager.addTaskRenderThread(() -> {
			lights.remove(l);
		});
	}

	public List<AreaLightInternal> getLights() {
		return lights;
	}

}
