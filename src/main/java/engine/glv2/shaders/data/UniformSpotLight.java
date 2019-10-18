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
		position.loadVec3(light.x, light.y, light.z);
		direction.loadVec3(light.direction);
		color.loadVec3(light.color);
		intensity.loadFloat(light.intensity);
		radius.loadFloat(light.radius);
		visible.loadBoolean(light.visible);
		outerFOV.loadFloat((float) Math.cos(Math.toRadians(light.outerFOV)));
		innerFOV.loadFloat((float) Math.cos(Math.toRadians(light.innerFOV * light.outerFOV)));
		viewMatrix.loadMatrix(light.getLightCamera().getViewMatrix());
		projectionMatrix.loadMatrix(light.getLightCamera().getProjectionMatrix());
		shadowMap.loadTexUnit(shadowUnit);
		shadows.loadBoolean(light.shadows);
	}

}
