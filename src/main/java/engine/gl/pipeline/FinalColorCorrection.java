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

import engine.gl.IRenderingData;
import engine.gl.PostProcesPass;
import engine.gl.PostProcessPipeline;
import engine.gl.RendererData;
import engine.gl.objects.Texture;
import engine.gl.pipeline.shaders.FinalColorCorrectionShader;

public class FinalColorCorrection extends PostProcesPass<FinalColorCorrectionShader> {

	public FinalColorCorrection() {
		super("FinalColorCorrection");
	}

	@Override
	protected FinalColorCorrectionShader setupShader() {
		return new FinalColorCorrectionShader();
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, FinalColorCorrectionShader shader) {
		shader.loadSaturation(rnd.saturation);
	}

	@Override
	protected void setupTextures(RendererData rnd, PostProcessPipeline pp, Texture[] auxTex) {
		pp.getMainTex().active(GL_TEXTURE0);
	}

}
