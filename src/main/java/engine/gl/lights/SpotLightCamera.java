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

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.Maths;

public class SpotLightCamera {

	private Matrix4f projectionMatrix;

	private Matrix4f viewMatrix = new Matrix4f();

	private Vector3f temp = new Vector3f();

	private static final Vector3f UP = new Vector3f(0, 1, 0);

	public SpotLightCamera(float fov, int resolution) {
		projectionMatrix = Maths.createProjectionMatrix(resolution, resolution, fov, 0.1f, 100f);
		viewMatrix = new Matrix4f();
	}

	public void update(Vector3f direction, Vector3f position) {
		temp.set(direction);
		viewMatrix.setLookAt(position, temp.add(position), UP);
	}

	public void setFov(float fov, int resolution) {
		projectionMatrix = Maths.createProjectionMatrix(resolution, resolution, fov, 0.1f, 100f);
	}

	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

}
