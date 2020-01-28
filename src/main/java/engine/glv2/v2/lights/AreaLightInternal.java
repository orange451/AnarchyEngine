/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.v2.lights;

import org.joml.Vector3f;

public class AreaLightInternal extends Light {

	public Vector3f direction = new Vector3f(1, 1, 1);
	public float sizeX = 1, sizeY = 1;

	public AreaLightInternal(Vector3f direction, Vector3f position, float intensity) {
		this.direction.set(direction);
		super.position.set(position);
		this.intensity = intensity;
	}

}
