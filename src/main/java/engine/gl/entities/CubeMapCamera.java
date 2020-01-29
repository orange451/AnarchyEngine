/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CubeMapCamera {

	private static final float NEAR_PLANE = 0.1f;
	private static final float FAR_PLANE = 1000f;
	private static final float FOV = 90;
	private static final float ASPECT_RATIO = 1f;

	private Matrix4f viewMatrix = new Matrix4f(), projectionMatrix = new Matrix4f();

	private Vector3f position = new Vector3f(), rotation = new Vector3f();

	private Vector3f negate = new Vector3f();

	private static final Vector3f X = new Vector3f(1, 0, 0);
	private static final Vector3f Y = new Vector3f(0, 1, 0);
	private static final Vector3f Z = new Vector3f(0, 0, 1);

	public CubeMapCamera(Vector3f position) {
		this.position = position;
		createProjectionMatrix();
	}

	public void switchToFace(int faceIndex) {
		switch (faceIndex) {
		case 0:
			rotation.x = 90;
			rotation.y = -90;
			rotation.z = -90;
			break;
		case 1:
			rotation.x = 90;
			rotation.y = 90;
			rotation.z = 90;
			break;
		case 2:
			rotation.x = -90;
			rotation.y = 0;
			rotation.z = 0;
			break;
		case 3:
			rotation.x = 90;
			rotation.y = 0;
			rotation.z = 0;
			break;
		case 4:
			rotation.x = 180;
			rotation.y = 0;
			rotation.z = 0;
			break;
		case 5:
			rotation.x = 0;
			rotation.y = 0;
			rotation.z = 180;
			break;
		}
		updateViewMatrix();
	}

	public Vector3f getRotation() {
		return rotation;
	}

	public Vector3f getPosition() {
		return position;
	}

	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	private void createProjectionMatrix() {
		float y_scale = (float) (1f / Math.tan(Math.toRadians(FOV / 2f)));
		float x_scale = y_scale / ASPECT_RATIO;
		float frustum_length = FAR_PLANE - NEAR_PLANE;
		projectionMatrix.identity();
		projectionMatrix.m00(x_scale);
		projectionMatrix.m11(y_scale);
		projectionMatrix.m22(-((FAR_PLANE + NEAR_PLANE) / frustum_length));
		projectionMatrix.m23(-1);
		projectionMatrix.m32(-((2 * NEAR_PLANE * FAR_PLANE) / frustum_length));
		projectionMatrix.m33(0);
	}

	private void updateViewMatrix() {
		viewMatrix = createViewMatrix(this);
	}

	public void setPosition(Vector3f position) {
		this.position = position;
	}

	private Matrix4f createViewMatrix(CubeMapCamera camera) {
		viewMatrix.identity();
		createViewMatrixRot(camera.rotation.x, camera.rotation.y, camera.rotation.z, viewMatrix);
		createViewMatrixPos(camera.position, viewMatrix);
		return viewMatrix;
	}

	private Matrix4f createViewMatrixPos(Vector3f pos, Matrix4f viewMatrix) {
		if (viewMatrix == null) {
			viewMatrix = new Matrix4f();
			viewMatrix.identity();
		}
		viewMatrix.translate(pos.negate(negate));
		return viewMatrix;
	}

	private Matrix4f createViewMatrixRot(float pitch, float yaw, float roll, Matrix4f viewMatrix) {
		if (viewMatrix == null) {
			viewMatrix = new Matrix4f();
			viewMatrix.identity();
		}
		viewMatrix.rotate((float) Math.toRadians(pitch), X);
		viewMatrix.rotate((float) Math.toRadians(yaw), Y);
		viewMatrix.rotate((float) Math.toRadians(roll), Z);
		return viewMatrix;
	}

}
