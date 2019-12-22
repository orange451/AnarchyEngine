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

import engine.gl.light.SpotLightInternal;

public class UniformSpotLight extends UniformObject {

	private UniformVec3 position, direction, color;
	private UniformFloat radius, intensity;
	private UniformBoolean visible;
	private UniformFloat outerFOV, innerFOV;
	private UniformMatrix4 viewMatrix, projectionMatrix;
	private UniformSampler shadowMap;
	private UniformBoolean shadows;

	public UniformSpotLight(String name) {
		position = new UniformVec3(name + ".position");
		direction = new UniformVec3(name + ".direction");
		color = new UniformVec3(name + ".color");
		radius = new UniformFloat(name + ".radius");
		intensity = new UniformFloat(name + ".intensity");
		visible = new UniformBoolean(name + ".visible");
		outerFOV = new UniformFloat(name + ".outerFOV");
		innerFOV = new UniformFloat(name + ".innerFOV");
		viewMatrix = new UniformMatrix4(name + ".viewMatrix");
		projectionMatrix = new UniformMatrix4(name + ".projectionMatrix");
		shadowMap = new UniformSampler(name + ".shadowMap");
		shadows = new UniformBoolean(name + ".shadows");
		super.storeUniforms(position, direction, color, radius, intensity, visible, outerFOV, innerFOV, viewMatrix,
				projectionMatrix, shadowMap, shadows);
	}

	public void loadLight(SpotLightInternal light, int shadowUnit) {
		position.loadVec3(light.position);
		direction.loadVec3(light.direction);
		color.loadVec3(light.color);
		intensity.loadFloat(light.intensity);
		radius.loadFloat(light.radius);
		visible.loadBoolean(light.visible);
		outerFOV.loadFloat((float) Math.cos(Math.toRadians(light.outerFOV * 0.5f)));
		innerFOV.loadFloat((float) Math.cos(Math.toRadians((light.innerFOV * light.outerFOV) * 0.5f)));
		viewMatrix.loadMatrix(light.getLightCamera().getViewMatrix());
		projectionMatrix.loadMatrix(light.getLightCamera().getProjectionMatrix());
		shadowMap.loadTexUnit(shadowUnit);
		shadows.loadBoolean(light.shadows);
	}

}
