package engine.glv2;

import engine.gl.light.DirectionalLightInternal;

public interface IDirectionalLightHandler {

	public void addLight(DirectionalLightInternal l);

	public void removeLight(DirectionalLightInternal l);

}
