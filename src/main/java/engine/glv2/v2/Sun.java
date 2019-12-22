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

import org.joml.Vector3f;

import engine.gl.light.DirectionalLightInternal;
import engine.lua.type.object.insts.DynamicSkybox;

public class Sun {

	private DirectionalLightInternal light;

	private Vector3f rotation = new Vector3f(45, 5, 0);
	private Vector3f lookAt = new Vector3f();
	private boolean added;

	public Sun() {
		light = new DirectionalLightInternal(rotation, 1.0f);
	}

	public void update(DynamicSkybox skybox) {
		if (skybox == null)
			return;
		rotation.z = -skybox.getTime() * 0.015f;
		float yaw = (float) Math.toRadians(rotation.x);
		float pitch = (float) Math.toRadians(rotation.y);
		float roll = (float) Math.toRadians(rotation.z);

		lookAt.x = (float) (-Math.cos(yaw) * Math.sin(pitch) * Math.sin(roll) - Math.sin(yaw) * Math.cos(roll));
		lookAt.y = (float) (-Math.sin(yaw) * Math.sin(pitch) * Math.sin(roll) + Math.cos(yaw) * Math.cos(roll));
		lookAt.z = (float) (Math.cos(pitch) * Math.sin(-roll));
		light.direction.set(lookAt);
	}

	public DirectionalLightInternal getLight() {
		return light;
	}

	public DirectionalLightInternal addLight() {
		if (added)
			return null;
		added = true;
		return light;
	}

	public DirectionalLightInternal removeLight() {
		if (!added)
			return null;
		added = false;
		return light;
	}

}
