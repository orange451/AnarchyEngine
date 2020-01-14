/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.shaders.data;

import engine.glv2.v2.lights.DirectionalLightInternal;

public class UniformDirectionalLight extends UniformObject {

	private UniformVec3 direction, color;
	private UniformFloat intensity;
	private UniformBoolean visible;
	private UniformMatrix4 viewMatrix;
	private UniformMatrix4 projectionMatrix[];
	private UniformSampler shadowMap;
	private UniformBoolean shadows;

	public UniformDirectionalLight(String name) {
		direction = new UniformVec3(name + ".direction");
		color = new UniformVec3(name + ".color");
		intensity = new UniformFloat(name + ".intensity");
		visible = new UniformBoolean(name + ".visible");
		viewMatrix = new UniformMatrix4(name + ".viewMatrix");
		shadowMap = new UniformSampler(name + ".shadowMap");
		shadows = new UniformBoolean(name + ".shadows");
		projectionMatrix = new UniformMatrix4[4];
		for (int x = 0; x < 4; x++)
			projectionMatrix[x] = new UniformMatrix4(name + ".projectionMatrix" + x);
		super.storeUniforms(projectionMatrix);
		super.storeUniforms(direction, intensity, visible, color, viewMatrix, shadowMap, shadows);
	}

	public void loadLight(DirectionalLightInternal light, int i, int offset) {
		direction.loadVec3(light.direction);
		color.loadVec3(light.color);
		intensity.loadFloat(light.intensity);
		visible.loadBoolean(light.visible);
		viewMatrix.loadMatrix(light.getLightCamera().getViewMatrix());
		for (int x = 0; x < 4; x++)
			projectionMatrix[x].loadMatrix(light.getLightCamera().getProjectionArray()[x]);
		shadowMap.loadTexUnit(offset + i);
		shadows.loadBoolean(light.shadows);
	}

	public void loadLight(DirectionalLightInternal light, int shadowUnit) {
		direction.loadVec3(light.direction);
		color.loadVec3(light.color);
		intensity.loadFloat(light.intensity);
		visible.loadBoolean(light.visible);
		viewMatrix.loadMatrix(light.getLightCamera().getViewMatrix());
		for (int x = 0; x < 4; x++)
			projectionMatrix[x].loadMatrix(light.getLightCamera().getProjectionArray()[x]);
		shadowMap.loadTexUnit(shadowUnit);
		shadows.loadBoolean(light.shadows);
	}

}
