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
import static org.lwjgl.opengl.GL15C.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;

import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.compute.shaders.ColorCorrectionShader;
import engine.gl.objects.Texture;

public class ColorCorrection extends ComputePass<ColorCorrectionShader> {

	public ColorCorrection() {
		super("ColorCorrection");
	}

	@Override
	protected ColorCorrectionShader setupShader() {
		return new ColorCorrectionShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, ColorCorrectionShader shader) {
		shader.loadExposure(rnd.exposure);
		shader.loadGamma(rnd.gamma);
	}

	@Override
	protected void setupTextures(RendererData rnd, ComputePipeline pi, Texture[] auxTex) {
		mainTex.image(0, 0, true, 0, GL_WRITE_ONLY, GL_RGBA16F);
		auxTex[0].active(GL_TEXTURE0);
	}

}
