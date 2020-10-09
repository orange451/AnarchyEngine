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

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE7;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL15C.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;
import static org.lwjgl.opengl.GL44C.glClearTexImage;

import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.compute.shaders.AmbientOcclusionShader;
import engine.gl.objects.Texture;

public class AmbientOcclusion extends ComputePass<AmbientOcclusionShader> {

	public AmbientOcclusion() {
		super("AmbientOcclusion", 0.5f);
	}

	@Override
	protected AmbientOcclusionShader setupShader() {
		return new AmbientOcclusionShader();
	}

	@Override
	protected void setupImage() {
		glClearTexImage(super.mainTex.getTexture(), 0, GL_RGBA, GL_FLOAT, new float[] { 0, 0, 0, 1 });
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, AmbientOcclusionShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
		shader.loadVoxelSize(rnd.vm.getSize() * 2.0f);
		shader.loadVoxelOffset(rnd.vm.getCameraOffset());
	}

	@Override
	protected void setupTextures(RendererData rnd, ComputePipeline dp, Texture[] auxTex) {
		mainTex.image(0, 0, true, 0, GL_WRITE_ONLY, GL_RGBA16F);
		dp.getNormalTex().active(GL_TEXTURE0);
		dp.getDepthTex().active(GL_TEXTURE1);
		dp.getMaskTex().active(GL_TEXTURE2);
		dp.getMotionTex().active(GL_TEXTURE3);
		rnd.dlh.getMainTex().active(GL_TEXTURE4);
		rnd.plh.getMainTex().active(GL_TEXTURE5);
		rnd.slh.getMainTex().active(GL_TEXTURE6);
		rnd.alh.getMainTex().active(GL_TEXTURE7);
		rnd.vm.getColor().active(GL_TEXTURE8);
	}

}
