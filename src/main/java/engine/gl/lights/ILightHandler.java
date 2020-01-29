package engine.gl.lights;

public interface ILightHandler<T extends Light> {

	public void addLight(T l);

	public void removeLight(T l);

}
