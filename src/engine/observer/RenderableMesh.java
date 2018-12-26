package engine.observer;

import org.joml.Matrix4f;

import engine.gl.MaterialGL;
import engine.gl.shader.BaseShader;

public interface RenderableMesh {
	public void render(BaseShader shader, Matrix4f worldMatrix, MaterialGL material);
}
