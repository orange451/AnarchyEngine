package engine.glv2.v2.lights;

import engine.gl.light.DirectionalLightInternal;

public interface IDirectionalLightHandler {

	public void addLight(DirectionalLightInternal l);

	public void removeLight(DirectionalLightInternal l);

}
