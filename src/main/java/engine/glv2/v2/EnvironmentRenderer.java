/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.v2;

import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;

import engine.glv2.entities.LayeredCubeCamera;
import engine.glv2.objects.Framebuffer;
import engine.glv2.objects.FramebufferBuilder;
import engine.glv2.objects.Texture;
import engine.glv2.objects.TextureBuilder;

public class EnvironmentRenderer {

	private Framebuffer framebuffer;
	private Texture cubeTex, depthTex;

	private LayeredCubeCamera layeredCubeCamera;

	public EnvironmentRenderer(int size) {
		generateFramebuffer(size);
		layeredCubeCamera = new LayeredCubeCamera();
	}

	public void renderIrradiance(SkyRenderer sr, Sun sun, IRenderingData rd, RendererData rnd) {
		layeredCubeCamera.setPosition(rd.camera.getPosition().getInternal());
		framebuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		sr.renderReflections(rnd, layeredCubeCamera, sun,  true, false, true);
		framebuffer.unbind();
	}

	public void renderReflections(SkyRenderer sr, Sun sun, IRenderingData rd, RendererData rnd,
			RenderingManager renderingManager) {
		layeredCubeCamera.setPosition(rd.camera.getPosition().getInternal());
		framebuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		renderingManager.renderReflections(rd, rnd, layeredCubeCamera); // TODO: Issue here with shadow sampler
		sr.renderReflections(rnd, layeredCubeCamera, sun, false, false, false);
		framebuffer.unbind();
	}

	public void dispose() {
		disposeFramebuffer();
	}

	public Texture getCubeTexture() {
		return cubeTex;
	}

	private void generateFramebuffer(int size) {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_CUBE_MAP).bindTexture();
		tb.sizeTexture(size, size);
		for (int i = 0; i < 6; i++)
			tb.texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB16F, 0, GL_RGB, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		cubeTex = tb.endTexture();

		tb.genTexture(GL_TEXTURE_CUBE_MAP).bindTexture();
		tb.sizeTexture(size, size);
		for (int i = 0; i < 6; i++)
			tb.texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_DEPTH_COMPONENT, 0, GL_DEPTH_COMPONENT,
					GL_UNSIGNED_BYTE, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		depthTex = tb.endTexture();

		FramebufferBuilder fb = new FramebufferBuilder();
		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(size, size);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, cubeTex, 0);
		fb.framebufferTexture(GL_DEPTH_ATTACHMENT, depthTex, 0);
		framebuffer = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		framebuffer.dispose();
		cubeTex.dispose();
		depthTex.dispose();
	}

}
