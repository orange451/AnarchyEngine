package engine.glv2.v2;

import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.GLResourceLoader;
import engine.glv2.Maths;
import engine.glv2.entities.CubeMapCamera;
import engine.glv2.objects.VAO;
import engine.glv2.shaders.DynamicSkyShader;
import engine.glv2.shaders.StaticSkyShader;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;

public class SkyRenderer {

	private final float SIZE = 1;

	private final float[] CUBE = { -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE };

	private VAO dome, cube;
	private DynamicSkyShader dynamicSkyShader;
	private StaticSkyShader staticSkyShader;
	private Vector3f pos;

	private Matrix4f infMat, regMat;

	private DynamicSkybox dynamicSky;
	private Skybox staticSky;

	public SkyRenderer(GLResourceLoader loader) {
		dome = loader.loadObj("SkyDome");
		pos = new Vector3f();
		infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
		regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
		dynamicSkyShader = new DynamicSkyShader();
		staticSkyShader = new StaticSkyShader();
		cube = VAO.create();
		cube.bind();
		cube.createAttribute(0, CUBE, 3, GL_STATIC_DRAW);
		cube.unbind();
		cube.setVertexCount(CUBE.length / 3);
	}

	public void render(Camera camera, Matrix4f projection, Vector3f lightDirection, boolean renderSun,
			boolean infScale) {
		if (dynamicSky != null) {
			glCullFace(GL_FRONT);
			dynamicSkyShader.start();
			dynamicSkyShader.loadCamera(camera, projection);
			dynamicSkyShader.loadDynamicSky(dynamicSky);
			dynamicSkyShader.loadLightPosition(lightDirection);
			dynamicSkyShader.renderSun(renderSun);
			if (infScale)
				dynamicSkyShader.loadTransformationMatrix(infMat);
			else
				dynamicSkyShader.loadTransformationMatrix(regMat);
			dome.bind(0, 1, 2);
			glDrawElements(GL_TRIANGLES, dome.getIndexCount(), GL_UNSIGNED_INT, 0);
			dome.unbind(0, 1, 2);
			dynamicSkyShader.stop();
			glCullFace(GL_BACK);
		} else if (staticSky != null) {
			staticSkyShader.start();
			staticSkyShader.loadCamera(camera, projection);
			staticSkyShader.loadSky(staticSky);
			if (infScale)
				staticSkyShader.loadTransformationMatrix(infMat);
			else
				staticSkyShader.loadTransformationMatrix(regMat);
			cube.bind(0);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, staticSky.getImage().getTexture().getID());
			glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
			cube.unbind(0);
			staticSkyShader.stop();
		}
	}

	public void render(CubeMapCamera camera, Vector3f lightDirection, boolean renderSun, boolean infScale) {
		if (dynamicSky != null) {
			glCullFace(GL_FRONT);
			dynamicSkyShader.start();
			dynamicSkyShader.loadCamera(camera);
			dynamicSkyShader.loadDynamicSky(dynamicSky);
			dynamicSkyShader.loadLightPosition(lightDirection);
			dynamicSkyShader.renderSun(renderSun);
			if (infScale)
				dynamicSkyShader.loadTransformationMatrix(infMat);
			else
				dynamicSkyShader.loadTransformationMatrix(regMat);
			dome.bind(0, 1, 2);
			glDrawElements(GL_TRIANGLES, dome.getIndexCount(), GL_UNSIGNED_INT, 0);
			dome.unbind(0, 1, 2);
			dynamicSkyShader.stop();
			glCullFace(GL_BACK);
		} else if (staticSky != null) {
			staticSkyShader.start();
			staticSkyShader.loadCamera(camera);
			staticSkyShader.loadSky(staticSky);
			if (infScale)
				staticSkyShader.loadTransformationMatrix(infMat);
			else
				staticSkyShader.loadTransformationMatrix(regMat);
			cube.bind(0);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, staticSky.getImage().getTexture().getID());
			glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
			cube.unbind(0);
			staticSkyShader.stop();
		}
	}

	public void dispose() {
		dome.dispose();
		dynamicSkyShader.dispose();
	}

	public void setDynamicSky(DynamicSkybox dynamicSky) {
		this.dynamicSky = dynamicSky;
		if (dynamicSky != null) {
			infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
		} else {
			infMat = Maths.createTransformationMatrix(pos, -90, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, -90, 0, 0, 990);
		}
	}

	public void setStaticSky(Skybox staticSky) {
		this.staticSky = staticSky;
		if (staticSky != null) {
			infMat = Maths.createTransformationMatrix(pos, -90, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, -90, 0, 0, 990);
		} else {
			infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
		}
	}

}
