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

import engine.InternalRenderThread;

public class SpotLightInternal extends Light {
	public float outerFOV = 80;
	public float innerFOV = 0.1f;
	public float radius = 8;
	public Vector3f direction = new Vector3f(1, 1, -1);
	public int shadowResolution = 1024;
	public boolean shadows = true;
	private SpotLightShadowMap shadowMap;
	private SpotLightCamera lightCamera;

	public SpotLightInternal(Vector3f direction, Vector3f position, float outerFOV, float innerFOV, float radius,
			float intensity) {
		this.direction.set(direction);
		this.setPosition(position);
		this.outerFOV = outerFOV;
		this.innerFOV = innerFOV;
		this.radius = radius;
		this.intensity = intensity;
	}

	public void init() {
		shadowMap = new SpotLightShadowMap(shadowResolution);
		lightCamera = new SpotLightCamera(outerFOV, shadowResolution);
		lightCamera.update(direction, getPosition());
	}

	public void update() {
		lightCamera.update(direction, getPosition());
	}

	public void setOuterFOV(float outerFOV) {
		this.outerFOV = outerFOV;
		lightCamera.setFov(outerFOV, shadowResolution);
	}

	public void setSize(int size) {
		this.shadowResolution = size;
		lightCamera.setFov(outerFOV, size);
		InternalRenderThread.runLater(() -> {
			shadowMap.resize(size);
		});
	}

	public void dispose() {
		shadowMap.dispose();
	}

	public SpotLightShadowMap getShadowMap() {
		return shadowMap;
	}

	public SpotLightCamera getLightCamera() {
		return lightCamera;
	}
}
