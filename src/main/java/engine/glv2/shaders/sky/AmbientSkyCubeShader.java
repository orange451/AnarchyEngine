package engine.glv2.shaders.sky;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.entities.LayeredCubeCamera;
import engine.glv2.shaders.ShaderProgram;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformVec3;

public class AmbientSkyCubeShader extends ShaderProgram {

	private UniformMatrix4[] viewMatrixCube = new UniformMatrix4[6];
	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");

	private UniformVec3 ambient = new UniformVec3("ambient");

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/sky/AmbientCube.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/sky/AmbientCube.gs", GL_GEOMETRY_SHADER));
		super.addShader(new Shader("assets/shaders/sky/AmbientCube.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		for (int i = 0; i < 6; i++)
			viewMatrixCube[i] = new UniformMatrix4("viewMatrixCube[" + i + "]");
		super.storeUniforms(viewMatrixCube);
		super.storeUniforms(projectionMatrix, transformationMatrix, ambient);
	}

	public void loadCamera(LayeredCubeCamera camera) {
		for (int i = 0; i < 6; i++)
			this.viewMatrixCube[i].loadMatrix(camera.getViewMatrix()[i]);
		this.projectionMatrix.loadMatrix(camera.getProjectionMatrix());
	}

	public void loadTransformationMatrix(Matrix4f mat) {
		transformationMatrix.loadMatrix(mat);
	}

	public void loadAmbient(Vector3f ambient) {
		this.ambient.loadVec3(ambient);
	}

}
