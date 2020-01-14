package engine.glv2.v2.lights;

public interface ILightHandler<T extends Light> {

	public void addLight(T l);

	public void removeLight(T l);

}
