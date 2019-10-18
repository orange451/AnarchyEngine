package engine.glv2.v2.lights;

import engine.gl.light.SpotLightInternal;

public interface ISpotLightHandler {

	public void addLight(SpotLightInternal l);

	public void removeLight(SpotLightInternal l);

}
