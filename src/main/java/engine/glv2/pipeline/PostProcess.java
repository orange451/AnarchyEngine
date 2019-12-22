/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.pipeline;

import engine.glv2.v2.PostProcessPipeline;

public class PostProcess extends PostProcessPipeline {

	public PostProcess(int width, int height) {
		super(width, height);
	}

	private FXAA fxaa; // Done
	private ChromaticAberration chromaticAberration; // Done
	private DepthOfField depthOfField;
	private FinalColorCorrection finalColorCorrection;

	@Override
	public void setupPasses() {
		finalColorCorrection = new FinalColorCorrection();
		super.passes.add(finalColorCorrection);

		chromaticAberration = new ChromaticAberration();
		super.passes.add(chromaticAberration);

		depthOfField = new DepthOfField();
		super.passes.add(depthOfField);

		fxaa = new FXAA();
		super.passes.add(fxaa);

	}

}
