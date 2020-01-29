/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.lights;

import org.joml.Vector3f;

import engine.InternalRenderThread;

public class DirectionalLightInternal extends Light {
	public Vector3f direction = new Vector3f(1, 1, 1);
	public int distance = 50;
	public int shadowResolution = 1024;
	public boolean shadows = true;
	private DirectionalLightShadowMap shadowMap;
	private DirectionalLightCamera lightCamera;

	public DirectionalLightInternal(Vector3f direction, float intensity) {
		this.direction.set(direction);
		this.intensity = intensity;
	}

	public void init() {
		shadowMap = new DirectionalLightShadowMap(shadowResolution);
		lightCamera = new DirectionalLightCamera(distance);
		lightCamera.update(direction, getPosition());
	}

	public void update() {
		lightCamera.update(direction, getPosition());
	}

	public void setShadowDistance(int distance) {
		this.distance = distance;
		lightCamera.setShadowDistance(distance);
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

	public DirectionalLightShadowMap getShadowMap() {
		return shadowMap;
	}

	public DirectionalLightCamera getLightCamera() {
		return lightCamera;
	}
}
