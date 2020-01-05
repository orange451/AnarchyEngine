package engine.glv2.v2.lights;

import engine.gl.light.Light;

public interface ILightHandler<T extends Light> {

	public void addLight(T l);

	public void removeLight(T l);

}
