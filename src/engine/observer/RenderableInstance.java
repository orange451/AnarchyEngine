package engine.observer;

import engine.gl.shader.BaseShader;

public interface RenderableInstance {
	public void render(BaseShader shader);
}
