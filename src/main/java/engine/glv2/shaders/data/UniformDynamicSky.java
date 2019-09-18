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

import engine.lua.type.object.insts.DynamicSkybox;

public class UniformDynamicSky extends UniformObject {

	private UniformFloat brightness, time, cloudHeight, cloudSpeed;

	public UniformDynamicSky(String name) {
		brightness = new UniformFloat(name + ".brightness");
		time = new UniformFloat(name + ".time");
		cloudHeight = new UniformFloat(name + ".cloudHeight");
		cloudSpeed = new UniformFloat(name + ".cloudSpeed");
		super.storeUniforms(brightness, time, cloudHeight, cloudSpeed);
	}

	public void loadLight(DynamicSkybox sky) {
		brightness.loadFloat(sky.getBrightness());
		time.loadFloat(sky.getTime());
		cloudHeight.loadFloat(sky.getCloudHeight());
		cloudSpeed.loadFloat(sky.getCloudSpeed());
	}

}
