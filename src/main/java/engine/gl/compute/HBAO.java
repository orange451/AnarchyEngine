/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.compute;

import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL15C.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;

import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.compute.shaders.HBAOShader;
import engine.gl.objects.Texture;

public class HBAO extends ComputePass<HBAOShader> {

	public HBAO() {
		super("HBAO", 1.0f);
	}

	@Override
	protected HBAOShader setupShader() {
		return new HBAOShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, HBAOShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
	}
	
	@Override
	protected void shaderResizeSetup(int width, int height, HBAOShader shader) {
		shader.loadInverseAspectRatio((float)height / (float)width);
	}

	@Override
	protected void setupTextures(RendererData rnd, ComputePipeline dp, Texture[] auxTex) {
		mainTex.image(0, 0, true, 0, GL_WRITE_ONLY, GL_RGBA16F);
		dp.getNormalTex().active(GL_TEXTURE0);
		dp.getDepthTex().active(GL_TEXTURE1);
		dp.getMaskTex().active(GL_TEXTURE2);
	}

}
