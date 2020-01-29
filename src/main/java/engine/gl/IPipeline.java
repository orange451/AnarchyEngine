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

import engine.gl.lights.AreaLightInternal;
import engine.gl.lights.DirectionalLightInternal;
import engine.gl.lights.ILightHandler;
import engine.gl.lights.PointLightInternal;
import engine.gl.lights.SpotLightInternal;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.observer.Renderable;
import engine.observer.RenderableWorld;
import lwjgui.scene.layout.StackPane;

public interface IPipeline extends Renderable {

	public void setRenderableWorld(RenderableWorld instance);

	public RenderableWorld getRenderableWorld();

	public void setEnabled(boolean enabled);

	public StackPane getDisplayPane();

	public void setSize(int width, int height);

	public ILightHandler<PointLightInternal> getPointLightHandler();

	public ILightHandler<DirectionalLightInternal> getDirectionalLightHandler();

	public ILightHandler<SpotLightInternal> getSpotLightHandler();

	public ILightHandler<AreaLightInternal> getAreaLightHandler();

	public void setDyamicSkybox(DynamicSkybox dynamicSkybox);

	public void setStaticSkybox(Skybox skybox);

	public void reloadStaticSkybox();

	public RenderingSettings getRenderSettings();

	public boolean isInitialized();

	public void init();

	public void dispose();
}
