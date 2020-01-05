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

import engine.gl.light.PointLightInternal;

public class UniformPointLight extends UniformObject {

	private UniformVec3 position, color;
	private UniformFloat radius, intensity;
	private UniformBoolean visible;
	private UniformMatrix4 projectionMatrix;
	private UniformSampler shadowMap;
	private UniformBoolean shadows;

	public UniformPointLight(String name) {
		position = new UniformVec3(name + ".position");
		color = new UniformVec3(name + ".color");
		radius = new UniformFloat(name + ".radius");
		intensity = new UniformFloat(name + ".intensity");
		visible = new UniformBoolean(name + ".visible");
		projectionMatrix = new UniformMatrix4(name + ".projectionMatrix");
		shadowMap = new UniformSampler(name + ".shadowMap");
		shadows = new UniformBoolean(name + ".shadows");
		super.storeUniforms(position, color, radius, intensity, visible, shadowMap, shadows, projectionMatrix);
	}

	public void loadLight(PointLightInternal light, int shadowUnit) {
		position.loadVec3(light.position);
		color.loadVec3(light.color);
		intensity.loadFloat(light.intensity);
		radius.loadFloat(light.radius);
		visible.loadBoolean(light.visible);
		projectionMatrix.loadMatrix(light.getLightCamera().getProjectionMatrix());
		shadowMap.loadTexUnit(shadowUnit);
		shadows.loadBoolean(light.shadows);
	}

}
