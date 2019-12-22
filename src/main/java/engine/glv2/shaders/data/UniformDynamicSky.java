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
