package engine.glv2.v2;

import engine.gl.light.PointLightInternal;

public interface IPointLightHandler {

	public void addLight(PointLightInternal l);

	public void removeLight(PointLightInternal l);

}
