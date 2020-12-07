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
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE7;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL15C.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;

import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.compute.shaders.SSDOShader;
import engine.gl.objects.Texture;

public class SSDO extends ComputePass<SSDOShader> {

	public SSDO() {
		super("SSDO", 0.5f);
	}

	@Override
	protected SSDOShader setupShader() {
		return new SSDOShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, SSDOShader shader) {
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
