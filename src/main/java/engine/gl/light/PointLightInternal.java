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

import engine.InternalRenderThread;
import engine.glv2.entities.LayeredCubeCamera;
import engine.glv2.v2.lights.PointLightShadowMap;

public class PointLightInternal extends Light {
	public float radius = 64;
	public int shadowResolution = 512;
	public boolean shadows = true;
	private PointLightShadowMap shadowMap;
	private LayeredCubeCamera lightCamera;

	public PointLightInternal(Vector3f position, float radius, float intensity) {
		this.position = position;
		this.radius = radius;
		this.intensity = intensity;
	}

	public void init() {
		shadowMap = new PointLightShadowMap(shadowResolution);
		lightCamera = new LayeredCubeCamera();
		lightCamera.setPosition(position);
	}

	public void update() {
		lightCamera.setPosition(position);
	}

	public void setSize(int size) {
		this.shadowResolution = size;
		InternalRenderThread.runLater(() -> {
			shadowMap.resize(size);
		});
	}

	public void dispose() {
		shadowMap.dispose();
	}

	public PointLightShadowMap getShadowMap() {
		return shadowMap;
	}

	public LayeredCubeCamera getLightCamera() {
		return lightCamera;
	}
}
