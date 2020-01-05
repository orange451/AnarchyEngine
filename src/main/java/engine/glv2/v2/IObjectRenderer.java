/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.v2;

import java.util.List;

import org.joml.Vector2f;

import engine.glv2.entities.LayeredCubeCamera;
import engine.glv2.v2.lights.DirectionalLightCamera;
import engine.glv2.v2.lights.SpotLightCamera;
import engine.lua.type.object.Instance;

public interface IObjectRenderer {

	public void preProcess(List<Instance> instances);

	public void render(IRenderingData rd, RendererData rnd, Vector2f resolution);

	public void renderReflections(IRenderingData rd, RendererData rnd, LayeredCubeCamera cubeCamera);

	public void renderForward(IRenderingData rd, RendererData rnd);

	public void renderShadow(DirectionalLightCamera camera);

	public void renderShadow(SpotLightCamera camera);

	public void renderShadow(LayeredCubeCamera camera);

	public void dispose();

	public void end();

	public int getID();

}
