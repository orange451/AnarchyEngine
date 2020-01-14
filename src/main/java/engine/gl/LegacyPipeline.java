/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl;

import java.util.HashMap;

import engine.observer.RenderableWorld;

@Deprecated
public class LegacyPipeline  {
	private static IPipeline currentPipeline;
	private static HashMap<RenderableWorld, IPipeline> pipelineMap = new HashMap<>();
	
	public static void set(IPipeline pipeline, RenderableWorld instance) {
		pipelineMap.put(instance, pipeline);
	}
	
	public static IPipeline get(RenderableWorld world) {
		return pipelineMap.get(world);
	}
	
	public static LegacyPipeline pipeline_get() {
		return (LegacyPipeline) currentPipeline;
	}
	
	public static IPipeline pipeline_get_v2() {
		return currentPipeline;
	}

}
