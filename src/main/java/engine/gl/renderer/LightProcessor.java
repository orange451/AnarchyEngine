/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */


package engine.gl.renderer;

import engine.gl.LegacyPipeline;
import engine.gl.PostProcessor;
import engine.gl.Surface;
import engine.gl.ibl.IBLHandler;
import engine.gl.light.PointLightHandler;
import lwjgui.paint.Color;

public class LightProcessor implements PostProcessor {
	private PointLightHandler pointLight = new PointLightHandler();
	private IBLHandler iblHandler = new IBLHandler();
	
	@Override
	public void process(LegacyPipeline pipeline) {
		Surface buffer = pipeline.getGBuffer().getAccumulationBuffer();
		
		buffer.bind();
		{
			buffer.draw_clear_alpha(Color.BLACK, 1.0f);
			
			// Draw IBL
			iblHandler.handle(pipeline);
			
			// Draw point lights
			pointLight.handle(pipeline);
		}
		buffer.unbind();
	}

	public PointLightHandler getPointLightHandler() {
		return pointLight;
	}
}
