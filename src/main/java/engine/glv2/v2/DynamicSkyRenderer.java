package engine.glv2.v2;

import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDrawElements;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.GLResourceLoader;
import engine.glv2.Maths;
import engine.glv2.entities.CubeMapCamera;
import engine.glv2.objects.VAO;
import engine.glv2.shaders.SkydomeShader;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;

public class DynamicSkyRenderer {

	private VAO dome;
	private SkydomeShader shader;
	private Vector3f pos;

	private Matrix4f infMat, regMat;

	private DynamicSkybox sky;

	public DynamicSkyRenderer(GLResourceLoader loader) {
		dome = loader.loadObj("SkyDome");
		pos = new Vector3f();
		infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
		regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
		shader = new SkydomeShader();
	}

	public void render(Camera camera, Matrix4f projection, Vector3f lightDirection, boolean renderSun,
			boolean infScale) {
		if (sky == null)
			return;
		glCullFace(GL_FRONT);
		shader.start();
		shader.loadCamera(camera, projection);
		shader.loadDynamicSky(sky);
		shader.loadLightPosition(lightDirection);
		shader.renderSun(renderSun);
		if (infScale)
			shader.loadTransformationMatrix(infMat);
		else
			shader.loadTransformationMatrix(regMat);
		dome.bind(0, 1, 2);
		glDrawElements(GL_TRIANGLES, dome.getIndexCount(), GL_UNSIGNED_INT, 0);
		dome.unbind(0, 1, 2);
		shader.stop();
		glCullFace(GL_BACK);
	}

	public void render(CubeMapCamera camera, Vector3f lightDirection, boolean renderSun, boolean infScale) {
		if (sky == null)
			return;
		glCullFace(GL_FRONT);
		shader.start();
		shader.loadCamera(camera);
		shader.loadDynamicSky(sky);
		shader.loadLightPosition(lightDirection);
		shader.renderSun(renderSun);
		if (infScale)
			shader.loadTransformationMatrix(infMat);
		else
			shader.loadTransformationMatrix(regMat);
		dome.bind(0, 1, 2);
		glDrawElements(GL_TRIANGLES, dome.getIndexCount(), GL_UNSIGNED_INT, 0);
		dome.unbind(0, 1, 2);
		shader.stop();
		glCullFace(GL_BACK);
	}

	public void dispose() {
		dome.dispose();
		shader.dispose();
	}

	public void setSky(DynamicSkybox sky) {
		this.sky = sky;
	}

}
