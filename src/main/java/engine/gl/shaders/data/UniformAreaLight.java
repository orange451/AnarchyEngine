/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.shaders.data;

import engine.gl.lights.AreaLightInternal;

public class UniformAreaLight extends UniformObject {

	private UniformVec3 position, direction, color;
	private UniformFloat intensity;
	private UniformBoolean visible;

	public UniformAreaLight(String name) {
		position = new UniformVec3(name + ".position");
		direction = new UniformVec3(name + ".direction");
		color = new UniformVec3(name + ".color");
		intensity = new UniformFloat(name + ".intensity");
		visible = new UniformBoolean(name + ".visible");
		super.storeUniforms(position, direction, color, intensity, visible);
	}

	public void loadLight(AreaLightInternal light) {
		position.loadVec3(light.position);
		direction.loadVec3(light.direction);
		color.loadVec3(light.color);
		intensity.loadFloat(light.intensity);
		visible.loadBoolean(light.visible);
	}

}
