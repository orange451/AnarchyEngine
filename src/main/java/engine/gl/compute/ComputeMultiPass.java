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

	private AmbientOcclusion ao;
	private Lighting lighting;

	private MotionBlur mblur;
	private ColorCorrection colorCorrection;
	private TAA taa;

	@Override
	public void setupPasses() {
		ao = new AmbientOcclusion();
		super.passes.add(ao);

		lighting = new Lighting();
		super.passes.add(lighting);

		mblur = new MotionBlur();
		super.passes.add(mblur);

		colorCorrection = new ColorCorrection();
		super.passes.add(colorCorrection);
		
		taa = new TAA();
		super.passes.add(taa);
	}

}
