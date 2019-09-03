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

public class UniformLight extends UniformObject {

	private UniformInteger type;
	private UniformVec3 position, color, direction;
	private UniformFloat radius, inRadius;
	private UniformBoolean useShadows;
	private UniformSampler shadowMap;
	private UniformMatrix4 viewMatrix, projectionMatrix;

	public UniformLight(String name) {
		type = new UniformInteger(name + ".type");
		position = new UniformVec3(name + ".position");
		color = new UniformVec3(name + ".color");
		direction = new UniformVec3(name + ".direction");
		radius = new UniformFloat(name + ".radius");
		inRadius = new UniformFloat(name + ".inRadius");
		useShadows = new UniformBoolean(name + ".useShadows");
		shadowMap = new UniformSampler(name + ".shadowMap");
		viewMatrix = new UniformMatrix4(name + ".viewMatrix");
		projectionMatrix = new UniformMatrix4(name + ".projectionMatrix");
		super.init(type, position, color, direction, radius, inRadius, useShadows, shadowMap, viewMatrix,
				projectionMatrix);
	}
/*
	public void loadLight(Light light, int offset, int number) {
		type.loadInteger(light.getType());
		position.loadVec3(light.getPosition());
		color.loadVec3(light.getColor());
		if (light.getType() == 1) {
			radius.loadFloat((float) Math.cos(Math.toRadians(light.getRadius())));
			inRadius.loadFloat((float) Math.cos(Math.toRadians(light.getInRadius())));
			direction.loadVec3(light.getDirection());
		}
		useShadows.loadBoolean(light.useShadows());
		if (light.useShadows()) {
			shadowMap.loadTexUnit(offset + number);
			viewMatrix.loadMatrix(light.getCamera().getViewMatrix());
			projectionMatrix.loadMatrix(light.getCamera().getProjectionMatrix());
		}
	}*/

}
