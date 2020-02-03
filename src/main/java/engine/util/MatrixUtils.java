/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.util;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.lua.type.object.insts.Camera;

public class MatrixUtils {
	private static final Vector3f POSITIVE = new Vector3f(1, 1, 1);

	public static Vector2f project3Dto2D(Vector3f input, Matrix4f projection, Matrix4f view, Vector2f size) {
		Matrix4f viewProjMat = new Matrix4f();

		// Calculate view-projection matrix
		projection.mul(view, viewProjMat);

		// Get view-space position
		Vector3f pos = new Vector3f(input.x, input.y, input.z);
		pos.mulProject(viewProjMat, pos);

		// Put in [0-1] space (currently in NDC space [-1 to 1])
		pos.y *= -1; // Invert Y for OpenGL
		pos = pos.add(POSITIVE);
		pos = pos.mul(0.5f);

		// Put into screen space
		pos.mul(new Vector3f(size.x, size.y, 1f));

		// Return it as vec2
		return new Vector2f(pos.x, pos.y);
	}

	public static Vector3f project2Dto3D(Vector2f screenSpaceCoordinates, Matrix4f projection, Camera camera,
			Vector2f size) {
		Matrix4f iProjMat = projection.invert(new Matrix4f());
		Matrix4f iViewMat = camera.getViewMatrixInverse().invert(new Matrix4f());

		// Put in Normalized Device Coordinate space [-1 to 1] Currently in Screen space
		// [0 to screen size]
		Vector2f ndc = screenSpaceCoordinates.mul(1 / (float) size.x, 1 / (float) size.y, new Vector2f());
		ndc.mul(2);
		ndc.sub(1, 1);
		ndc.y *= -1;

		Vector3f mCoords = new Vector3f(ndc.x, ndc.y, 1.0f);

		// Put Mouse-coords from NDC space into view space
		mCoords.mulProject(iProjMat);

		// Put Mouse-coords from view space into world space
		mCoords.mulProject(iViewMat);

		// Subtract cameras position ( World-space into Object space )
		Vector3f camPos = camera.getPosition().toJoml();
		Vector3f finalCoords = mCoords.sub(camPos, new Vector3f());

		// Normalize
		finalCoords.normalize();

		return finalCoords;
	}
}
