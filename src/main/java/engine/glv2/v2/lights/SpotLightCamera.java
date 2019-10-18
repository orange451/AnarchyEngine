package engine.glv2.v2.lights;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.Maths;

public class SpotLightCamera {

	private Matrix4f projectionMatrix;

	private Matrix4f viewMatrix = new Matrix4f();

	private Vector3f temp = new Vector3f();

	public SpotLightCamera(float fov, int resolution) {
		projectionMatrix = Maths.createProjectionMatrix(resolution, resolution, fov, 0.1f, 100f);
		viewMatrix = new Matrix4f();
	}

	public void update(Vector3f direction, Vector3f position) {
		temp.set(direction);
		viewMatrix.setLookAt(position, temp.add(position), new Vector3f(0, 1, 0));
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
