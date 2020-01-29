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

import engine.gl.DeferredPipeline;

public class MultiPass extends DeferredPipeline {

	public MultiPass(int width, int height) {
		super(width, height);
	}

	private Lighting lighting; // Done
	private VolumetricLight volumetricLight; // Done
	private BloomMask bloomMask; // Done
	private GaussianBlur gH1, gH2, gV1, gV2; // Done
	private Reflections reflections;
	private ColorCorrection colorCorrection; // Done
	private LensFlares lensFlares; // Done
	private LensFlareMod lensFlareMod; // Done
	private Bloom bloom; // Done
	private TAA taa; // Done
	private MotionBlur mblur;

	public void setupPasses() {
		//volumetricLight = new VolumetricLight(0.5f);
		//super.passes.add(volumetricLight);

		gH1 = new GaussianBlur(false, 0.5f);
		//super.passes.add(gH1);

		gV1 = new GaussianBlur(true, 0.5f);
		//super.passes.add(gV1);

		lighting = new Lighting();
		super.passes.add(lighting);

		reflections = new Reflections();
		super.passes.add(reflections);

		bloomMask = new BloomMask();
		super.passes.add(bloomMask);

		super.passes.add(gH1);
		super.passes.add(gV1);

		bloom = new Bloom();
		super.passes.add(bloom);

		lensFlares = new LensFlares();
		super.passes.add(lensFlares);

		lensFlareMod = new LensFlareMod();
		super.passes.add(lensFlareMod);

		colorCorrection = new ColorCorrection();
		super.passes.add(colorCorrection);

		mblur = new MotionBlur();
		super.passes.add(mblur);

		taa = new TAA();
		super.passes.add(taa);
	}

}
