package engine.observer;

import engine.gl.shader.BaseShader;
import engine.lua.type.data.Matrix4;
import engine.lua.type.object.insts.Prefab;

public interface RenderableInstance {
	public void render(BaseShader shader);
	public Prefab getPrefab();
	public Matrix4 getWorldMatrix();
}
