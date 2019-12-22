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
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_RG;
import static org.lwjgl.opengl.GL30C.GL_RG16F;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL30C.glFramebufferTexture2D;

import org.joml.Vector3f;

import engine.glv2.entities.CubeMapCamera;
import engine.glv2.objects.Framebuffer;
import engine.glv2.objects.FramebufferBuilder;
import engine.glv2.objects.Renderbuffer;
import engine.glv2.objects.RenderbufferBuilder;
import engine.glv2.objects.Texture;
import engine.glv2.objects.TextureBuilder;
import engine.glv2.objects.VAO;
import engine.glv2.shaders.BRDFIntegrationMapShader;
import engine.glv2.shaders.PreFilteredEnvironmentShader;

public class PreFilteredEnvironment {

	private final float SIZE = 1;

	private final float[] CUBE = { -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE };
	private final float[] QUAD = { -1, 1, -1, -1, 1, 1, 1, -1 };

	private PreFilteredEnvironmentShader shader;
	private BRDFIntegrationMapShader brdfIntegrationMapShader;
	private Texture cubeTex;
	private CubeMapCamera camera;
	private VAO cube, quad;
	private Texture brdfLUT;
	private Framebuffer framebuffer;
	private Renderbuffer depthBuffer;

	private int maxMipLevels = 5;

	public PreFilteredEnvironment() {

		shader = new PreFilteredEnvironmentShader();
		shader.init();
		camera = new CubeMapCamera(new Vector3f());

		quad = VAO.create();
		quad.bind();
		quad.createAttribute(0, QUAD, 2, GL_STATIC_DRAW);
		quad.unbind();
		quad.setVertexCount(4);
		cube = VAO.create();
		cube.bind();
		cube.createAttribute(0, CUBE, 3, GL_STATIC_DRAW);
		cube.unbind();
		cube.setVertexCount(CUBE.length / 3);

		generateFramebuffer(128);

		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(512, 512);
		tb.texImage2D(0, GL_RG16F, 0, GL_RG, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		brdfLUT = tb.endTexture();

		FramebufferBuilder fb = new FramebufferBuilder();
		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(512, 512);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, brdfLUT, 0);
		Framebuffer lutFB = fb.endFramebuffer();

		brdfIntegrationMapShader = new BRDFIntegrationMapShader();
		brdfIntegrationMapShader.init();

		lutFB.bind();
		brdfIntegrationMapShader.start();
		quad.bind(0);
		glDrawArrays(GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
		quad.unbind(0);
		brdfIntegrationMapShader.stop();
		lutFB.unbind();

		lutFB.dispose();
		brdfIntegrationMapShader.dispose();
		quad.dispose();
	}

	public void render(int envMap) {
		framebuffer.bind();
		shader.start();
		cube.bind(0);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_CUBE_MAP, envMap);
		shader.loadProjectionMatrix(camera.getProjectionMatrix());
		for (int mip = 0; mip < maxMipLevels; mip++) {
			int mipWidth = (int) ((float) cubeTex.getWidth() * Math.pow(0.5, mip));
			int mipHeight = (int) ((float) cubeTex.getHeight() * Math.pow(0.5, mip));
			glViewport(0, 0, mipWidth, mipHeight);

			float roughness = (float) mip / (float) (maxMipLevels - 1);
			shader.loadRoughness(roughness);
			for (int i = 0; i < 6; i++) {
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
						cubeTex.getTexture(), mip);
				camera.switchToFace(i);
				shader.loadviewMatrix(camera);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
			}
		}
		cube.unbind(0);
		shader.stop();
		framebuffer.unbind();
	}

	public void dispose() {
		cube.dispose();
		shader.dispose();
		brdfLUT.dispose();
		disposeFramebuffer();
	}

	public Texture getTexture() {
		return cubeTex;
	}

	public Texture getBRDFLUT() {
		return brdfLUT;
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
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		tb.texParameterf(GL_TEXTURE_LOD_BIAS, 0);
		tb.generateMipmap();
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
