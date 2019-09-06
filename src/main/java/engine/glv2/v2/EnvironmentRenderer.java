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
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL30C.glFramebufferTexture2D;

import org.joml.Vector3f;

import engine.glv2.RendererData;
import engine.glv2.RenderingManager;
import engine.glv2.SkydomeRenderer;
import engine.glv2.entities.CubeMapCamera;
import engine.glv2.objects.Framebuffer;
import engine.glv2.objects.FramebufferBuilder;
import engine.glv2.objects.Renderbuffer;
import engine.glv2.objects.RenderbufferBuilder;
import engine.glv2.objects.Texture;
import engine.glv2.objects.TextureBuilder;

public class EnvironmentRenderer {

	private Framebuffer framebuffer;
	private Renderbuffer depthBuffer;
	private Texture cubeTex;

	private CubeMapCamera camera;

	private int i;

	public EnvironmentRenderer(int size) {
		generateFramebuffer(size);
		camera = new CubeMapCamera(new Vector3f());
	}

	public void renderEnvironmentMap(Vector3f center, SkydomeRenderer sr, Vector3f lightPosition) {
		camera.setPosition(center);
		framebuffer.bind();
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
				cubeTex.getTexture(), 0);
		camera.switchToFace(i);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		sr.render(camera, /*ws,*/ lightPosition, true, false);
		framebuffer.unbind();
		i += 1;
		i %= 6;
	}

	public void renderEnvironmentMap(Vector3f center, SkydomeRenderer sr, RenderingManager renderingManager,
			IRenderingData rd, RendererData rnd) {
		camera.setPosition(center);
		framebuffer.bind();
		for (int i = 0; i < 6; i++) {
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
					cubeTex.getTexture(), 0);
			camera.switchToFace(i);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			renderingManager.renderReflections(rd, rnd, camera);
			sr.render(camera, /*rd.getWorldSimulation(),*/ rd.sun.getSunPosition(), false, false);
		}
		framebuffer.unbind();
	}

	public void dispose() {
		disposeFramebuffer();
	}

	public Texture getCubeTexture() {
		return cubeTex;
	}

	private void generateFramebuffer(int size) {
		RenderbufferBuilder rb = new RenderbufferBuilder();

		rb.genRenderbuffer().bindRenderbuffer().sizeRenderbuffer(size, size);
		rb.renderbufferStorage(GL_DEPTH_COMPONENT);
		depthBuffer = rb.endRenderbuffer();

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

		FramebufferBuilder fb = new FramebufferBuilder();
		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(size, size);
		fb.framebufferTexture2D(GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X, cubeTex, 0);
		fb.framebufferRenderbuffer(GL_DEPTH_ATTACHMENT, depthBuffer);
		framebuffer = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		framebuffer.dispose();
		depthBuffer.dispose();
		cubeTex.dispose();
	}

}
