/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.entities;

import org.joml.Matrix4f;
import org.joml.Rayf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.glv2.Maths;

public class SunCamera {

	private Vector2f center;

	private Matrix4f[] projectionArray;

	private Matrix4f viewMatrix = new Matrix4f(), projectionMatrix = new Matrix4f();

	private Vector3f position = new Vector3f(), rotation = new Vector3f();

	protected CastRay castRay;

	public SunCamera() {
		int shadowDrawDistance = 100;
		shadowDrawDistance *= 2;
		projectionArray = new Matrix4f[4];
		projectionArray[0] = Maths.orthoSymmetric(-shadowDrawDistance / 25, shadowDrawDistance / 25,
				-shadowDrawDistance, shadowDrawDistance, false);
		projectionArray[1] = Maths.orthoSymmetric(-shadowDrawDistance / 10, shadowDrawDistance / 10,
				-shadowDrawDistance, shadowDrawDistance, false);
		projectionArray[2] = Maths.orthoSymmetric(-shadowDrawDistance / 4, shadowDrawDistance / 4, -shadowDrawDistance,
				shadowDrawDistance, false);
		projectionArray[3] = Maths.orthoSymmetric(-shadowDrawDistance, shadowDrawDistance, -shadowDrawDistance,
				shadowDrawDistance, false);

		center = new Vector2f(1024, 1024);
		projectionMatrix = projectionArray[0];
		viewMatrix = createViewMatrix(this);
		castRay = new CastRay(projectionMatrix, viewMatrix, center, 2048, 2048);
	}

	public void updateShadowRay(boolean inverted) {
		viewMatrix = createViewMatrix(this);
		if (inverted)
			castRay.update(projectionMatrix,
					Maths.createViewMatrixPos(position,
							Maths.createViewMatrixRot(rotation.x() + 180, rotation.y(), rotation.z(), null)),
					center, 2048, 2048);
		else
			castRay.update(projectionMatrix, viewMatrix, center, 2048, 2048);
	}

	public void switchProjectionMatrix(int id) {
		projectionMatrix = this.projectionArray[id];
	}

	public void setProjectionArray(Matrix4f[] projectionArray) {
		this.projectionArray = projectionArray;
	}

	public Matrix4f[] getProjectionArray() {
		return projectionArray;
	}
	
	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	public void setRotation(Vector3f rotation) {
		this.rotation = rotation;
	}
	
	public CastRay getDRay() {
		return castRay;
	}

	private static Matrix4f createViewMatrix(SunCamera camera) {
		Matrix4f viewMatrix = new Matrix4f();
		viewMatrix.identity();
		createViewMatrixRot(camera.rotation.x, camera.rotation.y, 0, viewMatrix);
		createViewMatrixPos(camera.position, viewMatrix);
		return viewMatrix;
	}

	private static Matrix4f createViewMatrixPos(Vector3f pos, Matrix4f viewMatrix) {
		if (viewMatrix == null) {
			viewMatrix = new Matrix4f();
			viewMatrix.identity();
		}
		viewMatrix.translate(pos.negate(new Vector3f()));
		return viewMatrix;
	}

	private static Matrix4f createViewMatrixRot(float pitch, float yaw, float roll, Matrix4f viewMatrix) {
		if (viewMatrix == null) {
			viewMatrix = new Matrix4f();
			viewMatrix.identity();
		}
		viewMatrix.rotate((float) Math.toRadians(pitch), new Vector3f(1, 0, 0));
		viewMatrix.rotate((float) Math.toRadians(yaw), new Vector3f(0, 1, 0));
		viewMatrix.rotate((float) Math.toRadians(roll), new Vector3f(0, 0, 1));
		return viewMatrix;
	}

	public class CastRay {
		private Rayf ray;

		public CastRay(Matrix4f proj, Matrix4f view, Vector2f mouse, int width, int height) {
			Vector3f v = new Vector3f();
			v.x = (((2.0f * mouse.x) / width) - 1) / proj.m00();
			v.y = -(((2.0f * mouse.y) / height) - 1) / proj.m11();
			v.z = 1.0f;

			Matrix4f invertView = view.invert(new Matrix4f());

			Vector3f rayDirection = new Vector3f();
			rayDirection.x = v.x * invertView.m00() + v.y * invertView.m10() + v.z * invertView.m20();
			rayDirection.y = v.x * invertView.m01() + v.y * invertView.m11() + v.z * invertView.m21();
			rayDirection.z = v.x * invertView.m02() + v.y * invertView.m12() + v.z * invertView.m22();
			Vector3f rayOrigin = new Vector3f(invertView.m30(), invertView.m31(), invertView.m32());
			ray = new Rayf(rayOrigin, new Vector3f(-rayDirection.x, -rayDirection.y, -rayDirection.z));
		}

		public void update(Matrix4f proj, Matrix4f view, Vector2f mouse, int width, int height) {
			Vector3f v = new Vector3f();
			v.x = (((2.0f * mouse.x) / width) - 1) / proj.m00();
			v.y = -(((2.0f * mouse.y) / height) - 1) / proj.m11();
			v.z = 1.0f;

			Matrix4f invertView = view.invert(new Matrix4f());

			Vector3f rayDirection = new Vector3f();
			rayDirection.x = v.x * invertView.m00() + v.y * invertView.m10() + v.z * invertView.m20();
			rayDirection.y = v.x * invertView.m01() + v.y * invertView.m11() + v.z * invertView.m21();
			rayDirection.z = v.x * invertView.m02() + v.y * invertView.m12() + v.z * invertView.m22();
			Vector3f rayOrigin = new Vector3f(invertView.m30(), invertView.m31(), invertView.m32());
			ray = new Rayf(rayOrigin, new Vector3f(-rayDirection.x, -rayDirection.y, -rayDirection.z));
		}

		public Rayf getRay() {
			return ray;
		}

	}

}
