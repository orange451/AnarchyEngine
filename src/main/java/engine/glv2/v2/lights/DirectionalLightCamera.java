package engine.glv2.v2.lights;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.Maths;

public class DirectionalLightCamera {

	private Matrix4f[] projectionArray;

	private Matrix4f viewMatrix = new Matrix4f(), projectionMatrix = new Matrix4f();

	public DirectionalLightCamera(int distance) {
		int shadowDrawDistance = distance;
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

		projectionMatrix = projectionArray[0];
		viewMatrix = new Matrix4f();
	}

	public void update(Vector3f direction) {
		viewMatrix.lookAt(new Vector3f(0), direction, new Vector3f(0, 0, 1));
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

	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

}
