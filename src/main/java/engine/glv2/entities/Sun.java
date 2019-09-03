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

import org.joml.Vector3f;

public class Sun {

	private Vector3f rotation = new Vector3f(5, 0, 35);
	private Vector3f sunPosition = new Vector3f(0, 0, 0);
	private Vector3f invertedSunPosition = new Vector3f(0, 0, 0);
	private SunCamera camera;

	public Sun() {
		camera = new SunCamera();
	}

	public Sun(Vector3f rotation) {
		this.rotation = rotation;
		camera = new SunCamera();
	}

	public void update(float rot, float delta) {
		rotation.y = rot;
		camera.setRotation(new Vector3f(rotation.y, rotation.x, rotation.z));
		camera.updateShadowRay(true);
		sunPosition.set(camera.getDRay().getRay().dX * 10, camera.getDRay().getRay().dY * 10,
				camera.getDRay().getRay().dZ * 10);

		camera.updateShadowRay(false);
		invertedSunPosition.set(camera.getDRay().getRay().dX * 10, camera.getDRay().getRay().dY * 10,
				camera.getDRay().getRay().dZ * 10);
	}

	public SunCamera getCamera() {
		return camera;
	}

	public Vector3f getInvertedSunPosition() {
		return invertedSunPosition;
	}

	public Vector3f getSunPosition() {
		return sunPosition;
	}

}
