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

import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glDrawElements;
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
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL30C.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.entities.CubeMapCamera;
import engine.gl.entities.LayeredCubeCamera;
import engine.gl.objects.Framebuffer;
import engine.gl.objects.FramebufferBuilder;
import engine.gl.objects.Renderbuffer;
import engine.gl.objects.RenderbufferBuilder;
import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;
import engine.gl.objects.VAO;
import engine.gl.shaders.SphereToCubeShader;
import engine.gl.shaders.sky.AmbientSkyCubeShader;
import engine.gl.shaders.sky.AmbientSkyShader;
import engine.gl.shaders.sky.DynamicSkyCubeShader;
import engine.gl.shaders.sky.DynamicSkyShader;
import engine.gl.shaders.sky.StaticSkyCubeShader;
import engine.gl.shaders.sky.StaticSkyShader;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.tasks.TaskManager;

public class SkyRenderer {

	private final float SIZE = 1;

	private final float[] CUBE = { -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE };

	private VAO dome, cube;
	private DynamicSkyShader dynamicSkyShader;
	private StaticSkyShader staticSkyShader;
	private AmbientSkyShader ambientSkyShader;

	private AmbientSkyCubeShader ambientSkyCubeShader;
	private StaticSkyCubeShader staticSkyCubeShader;
	private DynamicSkyCubeShader dynamicSkyCubeShader;
	private Vector3f pos;

	private Matrix4f infMat, regMat;

	private DynamicSkybox dynamicSky;
	private Skybox staticSky;

	private Texture cubeTex;
	private Framebuffer framebuffer;
	private Renderbuffer depthBuffer;

	private static final Vector3f AMBIENT = new Vector3f(1.0f);

	public SkyRenderer(GLResourceLoader loader) {
		dome = loader.loadObj("SkyDome");
		pos = new Vector3f();
		infMat = Maths.createTransformationMatrix(pos, -90, 0, 0, Integer.MAX_VALUE);
		regMat = Maths.createTransformationMatrix(pos, -90, 0, 0, 990);
		dynamicSkyShader = new DynamicSkyShader();
		dynamicSkyShader.init();
		staticSkyShader = new StaticSkyShader();
		staticSkyShader.init();
		ambientSkyShader = new AmbientSkyShader();
		ambientSkyShader.init();

		ambientSkyCubeShader = new AmbientSkyCubeShader();
		ambientSkyCubeShader.init();
		staticSkyCubeShader = new StaticSkyCubeShader();
		staticSkyCubeShader.init();
		dynamicSkyCubeShader = new DynamicSkyCubeShader();
		dynamicSkyCubeShader.init();

		cube = VAO.create();
		cube.bind();
		cube.createAttribute(0, CUBE, 3, GL_STATIC_DRAW);
		cube.unbind();
		cube.setVertexCount(CUBE.length / 3);
	}

	public void render(RendererData rnd, IRenderingData rd, Vector3f lightDirection, boolean renderSun,
			boolean infScale) {
		if (dynamicSky != null) {
			glCullFace(GL_FRONT);
			dynamicSkyShader.start();
			dynamicSkyShader.loadCamera(rd.camera, rd.projectionMatrix);
			dynamicSkyShader.loadDynamicSky(dynamicSky);
			dynamicSkyShader.loadLightPosition(lightDirection);
			dynamicSkyShader.renderSun(renderSun);
			dynamicSkyShader.loadAmbient(AMBIENT);
			dynamicSkyShader.loadCameraPrev(rnd.previousViewMatrix, rnd.previousProjectionMatrix);
			if (infScale)
				dynamicSkyShader.loadTransformationMatrix(infMat);
			else
				dynamicSkyShader.loadTransformationMatrix(regMat);
			dome.bind(0, 1, 2);
			glDrawElements(GL_TRIANGLES, dome.getIndexCount(), GL_UNSIGNED_INT, 0);
			dome.unbind(0, 1, 2);
			dynamicSkyShader.stop();
			glCullFace(GL_BACK);
		} else if (staticSky != null) {
			if (staticSky.getImage() != null) {
				if (staticSky.getImage().hasLoaded()) {
					staticSkyShader.start();
					staticSkyShader.loadCamera(rd.camera, rd.projectionMatrix);
					staticSkyShader.loadSky(staticSky);
					staticSkyShader.loadAmbient(AMBIENT);
					staticSkyShader.loadCameraPrev(rnd.previousViewMatrix, rnd.previousProjectionMatrix);
					if (infScale)
						staticSkyShader.loadTransformationMatrix(infMat);
					else
						staticSkyShader.loadTransformationMatrix(regMat);
					cube.bind(0);
					glActiveTexture(GL_TEXTURE0);
					glBindTexture(GL_TEXTURE_CUBE_MAP, cubeTex.getTexture());
					glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
					cube.unbind(0);
					staticSkyShader.stop();
				}
			}
		} else {
			ambientSkyShader.start();
			ambientSkyShader.loadCamera(rd.camera, rd.projectionMatrix);
			ambientSkyShader.loadAmbient(rnd.ambient);
			ambientSkyShader.loadCameraPrev(rnd.previousViewMatrix, rnd.previousProjectionMatrix);
			if (infScale)
				ambientSkyShader.loadTransformationMatrix(infMat);
			else
				ambientSkyShader.loadTransformationMatrix(regMat);
			cube.bind(0);
			glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
			cube.unbind(0);
			ambientSkyShader.stop();
		}
	}

	public void renderReflections(RendererData rnd, LayeredCubeCamera camera, Sun sun, boolean renderSun,
			boolean infScale, boolean applyAmbient) {
		if (dynamicSky != null) {
			glCullFace(GL_FRONT);
			dynamicSkyCubeShader.start();
			dynamicSkyCubeShader.loadCamera(camera);
			dynamicSkyCubeShader.loadDynamicSky(dynamicSky);
			dynamicSkyCubeShader.loadLightPosition(sun.getLight().direction);
			dynamicSkyCubeShader.renderSun(renderSun);
			if (applyAmbient)
				dynamicSkyCubeShader.loadAmbient(rnd.ambient);
			else
				dynamicSkyCubeShader.loadAmbient(AMBIENT);
			if (infScale)
				dynamicSkyCubeShader.loadTransformationMatrix(infMat);
			else
				dynamicSkyCubeShader.loadTransformationMatrix(regMat);
			dome.bind(0, 1, 2);
			glDrawElements(GL_TRIANGLES, dome.getIndexCount(), GL_UNSIGNED_INT, 0);
			dome.unbind(0, 1, 2);
			dynamicSkyCubeShader.stop();
			glCullFace(GL_BACK);

		} else if (staticSky != null) {
			if (staticSky.getImage() != null) {
				if (staticSky.getImage().hasLoaded()) {
					staticSkyCubeShader.start();
					staticSkyCubeShader.loadCamera(camera);
					staticSkyCubeShader.loadSky(staticSky);
					if (applyAmbient)
						staticSkyCubeShader.loadAmbient(rnd.ambient);
					else
						staticSkyCubeShader.loadAmbient(AMBIENT);
					if (infScale)
						staticSkyCubeShader.loadTransformationMatrix(infMat);
					else
						staticSkyCubeShader.loadTransformationMatrix(regMat);
					cube.bind(0);
					glActiveTexture(GL_TEXTURE0);
					glBindTexture(GL_TEXTURE_CUBE_MAP, cubeTex.getTexture());
					glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
					cube.unbind(0);
					staticSkyCubeShader.stop();
				}
			}
		} else {
			ambientSkyCubeShader.start();
			ambientSkyCubeShader.loadCamera(camera);
			ambientSkyCubeShader.loadAmbient(rnd.ambient);
			if (infScale)
				ambientSkyCubeShader.loadTransformationMatrix(infMat);
			else
				ambientSkyCubeShader.loadTransformationMatrix(regMat);
			cube.bind(0);
			glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
			cube.unbind(0);
			ambientSkyCubeShader.stop();
		}
	}

	public void dispose() {
		dome.dispose();
		dynamicSkyShader.dispose();
		staticSkyShader.dispose();
		ambientSkyShader.dispose();

		ambientSkyCubeShader.dispose();
		staticSkyCubeShader.dispose();
		dynamicSkyCubeShader.dispose();
	}

	public void setDynamicSky(DynamicSkybox dynamicSky) {
		if (this.dynamicSky == dynamicSky)
			return;
		this.dynamicSky = dynamicSky;
		if (dynamicSky != null) {
			infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
		} else {
			infMat = Maths.createTransformationMatrix(pos, -90, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, -90, 0, 0, 990);
		}
	}

	public void setStaticSky(Skybox staticSky) {
		if (this.staticSky == staticSky)
			return;
		this.staticSky = staticSky;
		if (staticSky != null) {
			infMat = Maths.createTransformationMatrix(pos, -90, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, -90, 0, 0, 990);

			TextureBuilder tb = new TextureBuilder();

			tb.genTexture(GL_TEXTURE_CUBE_MAP).bindTexture();
			tb.sizeTexture(512, 512);
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
		} else {
			infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
			TaskManager.addTaskRenderThread(() -> cubeTex.dispose());
		}
	}

	public void reloadStaticSkybox() {
		generateFramebuffer(512);
		CubeMapCamera camera = new CubeMapCamera(new Vector3f());
		SphereToCubeShader stc = new SphereToCubeShader();
		stc.init();

		glDisable(GL_BLEND);
		
		framebuffer.bind();
		stc.start();
		cube.bind(0);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, staticSky.getImage().getTexture().getTexture());
		stc.loadProjectionMatrix(camera.getProjectionMatrix());
		for (int i = 0; i < 6; i++) {
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
					cubeTex.getTexture(), 0);
			camera.switchToFace(i);
			stc.loadviewMatrix(camera.getViewMatrix());
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
		}
		cube.unbind(0);
		stc.stop();
		framebuffer.unbind();

		glBindTexture(GL_TEXTURE_CUBE_MAP, cubeTex.getTexture());
		glGenerateMipmap(GL_TEXTURE_CUBE_MAP);
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
		stc.dispose();
		disposeFramebuffer();
	}

	private void generateFramebuffer(int size) {
		RenderbufferBuilder rb = new RenderbufferBuilder();

		rb.genRenderbuffer().bindRenderbuffer().sizeRenderbuffer(size, size);
		rb.renderbufferStorage(GL_DEPTH_COMPONENT);
		depthBuffer = rb.endRenderbuffer();

		FramebufferBuilder fb = new FramebufferBuilder();
		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(size, size);
		fb.framebufferTexture2D(GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X, cubeTex, 0);
		fb.framebufferRenderbuffer(GL_DEPTH_ATTACHMENT, depthBuffer);
		framebuffer = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		framebuffer.dispose();
		depthBuffer.dispose();
	}

}
