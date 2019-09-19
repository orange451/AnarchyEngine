package engine.gl.light;

import org.joml.Vector3f;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.glv2.v2.lights.DirectionalLightCamera;
import engine.glv2.v2.lights.DirectionalLightShadowMap;

public class DirectionalLightInternal extends Light {
	public Vector3f direction = new Vector3f(1, 1, 1);
	public int distance = 100;
	public int shadowResolution = 512;
	private DirectionalLightShadowMap shadowMap;
	private DirectionalLightCamera lightCamera;

	public DirectionalLightInternal(Vector3f direction, float intensity) {
		this.direction.set(direction);
		this.intensity = intensity;
		
		// Light may be created before game is setup (on load), so Game.Lighting is not available yet. Next frame it will be.
		InternalGameThread.runLater(() -> {
			this.shadowResolution = Game.lighting().getShadowMapSize();
		});
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
		if (shadowMap == null)
			return;

		shadowMap.dispose();
	}

	public DirectionalLightShadowMap getShadowMap() {
		return shadowMap;
	}

	public DirectionalLightCamera getLightCamera() {
		return lightCamera;
	}
}
