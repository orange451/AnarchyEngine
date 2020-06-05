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
import static org.lwjgl.opengl.GL13C.GL_TEXTURE10;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE11;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE12;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE9;

import engine.gl.DeferredPass;
import engine.gl.DeferredPipeline;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.objects.Texture;
import engine.gl.pipeline.shaders.AmbientOcclusionShader;

public class AmbientOcclusion extends DeferredPass<AmbientOcclusionShader> {

	public AmbientOcclusion() {
		super("AmbientOcclusion");
	}

	@Override
	protected AmbientOcclusionShader setupShader() {
		return new AmbientOcclusionShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, AmbientOcclusionShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		dp.getDiffuseTex().active(GL_TEXTURE0);
		dp.getNormalTex().active(GL_TEXTURE1);
		dp.getDepthTex().active(GL_TEXTURE2);
		dp.getPbrTex().active(GL_TEXTURE3);
		dp.getMaskTex().active(GL_TEXTURE4);
		rnd.dlh.getMainTex().active(GL_TEXTURE8);
		rnd.plh.getMainTex().active(GL_TEXTURE9);
		rnd.slh.getMainTex().active(GL_TEXTURE10);
		rnd.alh.getMainTex().active(GL_TEXTURE11);
		rnd.vm.getTexture().active(GL_TEXTURE12);
	}

}
