package engine.observer;

import engine.gl.shader.BaseShader;
import luaengine.type.data.Matrix4;
import luaengine.type.object.insts.Prefab;

public interface RenderableInstance {
	public void render(BaseShader shader);
	public Prefab getPrefab();
	public Matrix4 getWorldMatrix();
}
