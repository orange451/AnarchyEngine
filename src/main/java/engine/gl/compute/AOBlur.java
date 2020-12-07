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
import static org.lwjgl.opengl.GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;

import org.joml.Vector2f;

import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.compute.shaders.AOBlurShader;
import engine.gl.objects.Texture;

public class AOBlur extends ComputePass<AOBlurShader> {

	private final Vector2f H = new Vector2f(), V = new Vector2f();

	public AOBlur() {
		super("AOBlur", 1.0f);
	}

	@Override
	protected AOBlurShader setupShader() {
		return new AOBlurShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, AOBlurShader shader) {
		shader.loadCameraData(rd.projectionMatrix);
	}

	@Override
	protected void shaderResizeSetup(int width, int height, AOBlurShader shader) {
		H.set(1.0 / width, 0);
		V.set(0, 1.0 / height);
	}

	@Override
	protected void setupTextures(RendererData rnd, ComputePipeline dp, Texture[] auxTex) {
		mainTex.image(0, 0, true, 0, GL_WRITE_ONLY, GL_RGBA16F);
		auxTex[0].active(GL_TEXTURE0);
		dp.getDepthTex().active(GL_TEXTURE1);
		dp.getMaskTex().active(GL_TEXTURE2);
	}

	@Override
	protected void dispatch(int width, int height, AOBlurShader shader) {
		shader.loadDirection(H);
		shader.dispatch(width / 16, height / 16, 1);
		glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
		shader.loadDirection(V);
		shader.dispatch(width / 16, height / 16, 1);
	}

}
