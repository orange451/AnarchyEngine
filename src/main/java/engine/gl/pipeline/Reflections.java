/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.pipeline;

import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE7;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;

import engine.gl.DeferredPass;
import engine.gl.DeferredPipeline;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.objects.Texture;
import engine.gl.pipeline.shaders.ReflectionsShader;

public class Reflections extends DeferredPass<ReflectionsShader> {

	public Reflections() {
		super("Reflections");
	}

	@Override
	protected ReflectionsShader setupShader() {
		return new ReflectionsShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, ReflectionsShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		dp.getDiffuseTex().active(GL_TEXTURE0);
		dp.getNormalTex().active(GL_TEXTURE1);
		dp.getDepthTex().active(GL_TEXTURE2);
		dp.getPbrTex().active(GL_TEXTURE3);
		dp.getMaskTex().active(GL_TEXTURE4);
		rnd.environmentMap.active(GL_TEXTURE5);
		rnd.brdfLUT.active(GL_TEXTURE6);
		auxTex[0].active(GL_TEXTURE7);
		auxTex[1].active(GL_TEXTURE8);
	}

}
