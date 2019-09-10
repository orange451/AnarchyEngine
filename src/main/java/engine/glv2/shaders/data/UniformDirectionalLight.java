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

import engine.gl.light.DirectionalLightInternal;

public class UniformDirectionalLight extends UniformObject {

	private UniformVec3 direction, color;
	private UniformFloat intensity;
	private UniformBoolean visible;

	public UniformDirectionalLight(String name) {
		direction = new UniformVec3(name + ".direction");
		color = new UniformVec3(name + ".color");
		intensity = new UniformFloat(name + ".intensity");
		visible = new UniformBoolean(name + ".visible");
		super.init(direction, intensity, visible, color);
	}

	public void loadLight(DirectionalLightInternal light) {
		direction.loadVec3(light.direction);
		color.loadVec3(light.color);
		intensity.loadFloat(light.intensity);
		visible.loadBoolean(light.visible);
	}

}
