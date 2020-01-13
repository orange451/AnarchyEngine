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

import engine.gl.light.DirectionalLightInternal;
import engine.gl.light.PointLightInternal;
import engine.gl.light.SpotLightInternal;
import engine.glv2.v2.RenderingSettings;
import engine.glv2.v2.lights.ILightHandler;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.observer.RenderableWorld;

@Deprecated
public class LegacyPipeline implements IPipeline {
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

	@Override
	public void render() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRenderableWorld(RenderableWorld instance) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RenderableWorld getRenderableWorld() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Surface getPipelineBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSize(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ILightHandler<PointLightInternal> getPointLightHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ILightHandler<DirectionalLightInternal> getDirectionalLightHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ILightHandler<SpotLightInternal> getSpotLightHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDyamicSkybox(DynamicSkybox dynamicSkybox) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStaticSkybox(Skybox skybox) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reloadStaticSkybox() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RenderingSettings getRenderSettings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInitialized() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
