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

public class ComputeMultiPass extends ComputePipeline {

	public ComputeMultiPass(int width, int height) {
		super(width, height);
	}

	@Override
	public void setupPasses() {
		super.addStep(PipelineSteps.AMBIENT_OCCLUSION, new HBAO());
		super.addStep(PipelineSteps.AO_BLUR, new AOBlur());
		super.addStep(PipelineSteps.LIGHTING, new Lighting());
		super.addStep(PipelineSteps.MOTION_BLUR, new MotionBlur());
		super.addStep(PipelineSteps.COLOR_CORRECTION, new ColorCorrection());
		super.addStep(PipelineSteps.ANTI_ALIASING, new TAA());
	}

}
