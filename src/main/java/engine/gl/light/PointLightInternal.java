/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.light;

import org.joml.Vector3f;

public class PointLightInternal extends Light {
	public float radius = 64;
	
	public PointLightInternal(Vector3f position, float radius, float intensity) {
		this.position = position;
		this.radius = radius;
		this.intensity = intensity;
	}
}
