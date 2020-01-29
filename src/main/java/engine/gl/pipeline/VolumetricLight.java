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

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;

import engine.gl.DeferredPass;
import engine.gl.DeferredPipeline;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.objects.Texture;
import engine.gl.pipeline.shaders.VolumetricLightShader;

public class VolumetricLight extends DeferredPass<VolumetricLightShader> {

	public VolumetricLight(float scaling) {
		super("VolumetricLight", scaling);
	}

	@Override
	protected VolumetricLightShader setupShader() {
		return new VolumetricLightShader(name);
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, VolumetricLightShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
		shader.loadTime(0);
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		//super.activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, dp.getPositionTex().getTexture());
		//super.activateTexture(GL_TEXTURE1, GL_TEXTURE_2D, dp.getNormalTex().getTexture());
	}

}
