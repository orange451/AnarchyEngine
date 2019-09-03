/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
	private MotionBlur motionBlur; // Done
	private DepthOfField depthOfField;

	@Override
	public void setupPasses() {
		chromaticAberration = new ChromaticAberration();
		super.passes.add(chromaticAberration);

		depthOfField = new DepthOfField();
		super.passes.add(depthOfField);

		motionBlur = new MotionBlur();
		super.passes.add(motionBlur);

		fxaa = new FXAA();
		super.passes.add(fxaa);

	}

}
