package engine.glv2.v2.lights;

import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL30C.GL_TEXTURE_2D_ARRAY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;

import engine.gl.light.DirectionalLightInternal;
import engine.glv2.objects.Framebuffer;
import engine.glv2.objects.FramebufferBuilder;
import engine.glv2.objects.Texture;
import engine.glv2.objects.TextureBuilder;
import engine.glv2.objects.VAO;
import engine.glv2.shaders.DirectionalLightShader;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.RenderingSettings;
import engine.lua.type.object.insts.Camera;

public class DirectionalLightHandler implements IDirectionalLightHandler {

	private List<DirectionalLightInternal> lights = Collections.synchronizedList(new ArrayList<>());

	private VAO quad;

	private Framebuffer main;
	private Texture mainTex;

	private DirectionalLightShader shader;

	private int width, height;

	public DirectionalLightHandler(int width, int height) {
		this.width = width;
		this.height = height;
		init();
	}

	public void init() {
		float[] positions = { -1, 1, -1, -1, 1, 1, 1, -1 };
		quad = VAO.create();
		quad.bind();
		quad.createAttribute(0, positions, 2, GL_STATIC_DRAW);
		quad.unbind();
		quad.setVertexCount(4);
		shader = new DirectionalLightShader();
		generateFramebuffer();
	}

	public void render(Camera camera, Matrix4f projectionMatrix, DeferredPipeline dp, RenderingSettings rs) {
		main.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE);
		shader.start();
		shader.loadCameraData(camera, projectionMatrix);
		shader.loadUseShadows(rs.shadowsEnabled);
		quad.bind(0);
		activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, dp.getDiffuseTex().getTexture());
		//activateTexture(GL_TEXTURE1, GL_TEXTURE_2D, dp.getPositionTex().getTexture());
		activateTexture(GL_TEXTURE2, GL_TEXTURE_2D, dp.getNormalTex().getTexture());
		activateTexture(GL_TEXTURE3, GL_TEXTURE_2D, dp.getDepthTex().getTexture());
		activateTexture(GL_TEXTURE4, GL_TEXTURE_2D, dp.getPbrTex().getTexture());
		activateTexture(GL_TEXTURE5, GL_TEXTURE_2D, dp.getMaskTex().getTexture());
		synchronized (lights) {
			for (DirectionalLightInternal l : lights) {
				if (!l.visible)
					continue;
				shader.loadDirectionalLight(l);
				activateTexture(GL_TEXTURE6, GL_TEXTURE_2D_ARRAY, l.getShadowMap().getShadowMaps().getTexture());
				glDrawArrays(GL_TRIANGLE_STRIP, 0, quad.getVertexCount());
			}
		}
		quad.unbind(0);
		shader.stop();
		glDisable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		main.unbind();
	}

	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		disposeFramebuffer();
		generateFramebuffer();
	}

	public void dispose() {
		disposeFramebuffer();
		quad.dispose();
	}

	private void activateTexture(int textureNum, int target, int texture) {
		glActiveTexture(textureNum);
		glBindTexture(target, texture);
	}

	private void generateFramebuffer() {
		TextureBuilder tb = new TextureBuilder();

		tb.genTexture(GL_TEXTURE_2D).bindTexture();
		tb.sizeTexture(width, height).texImage2D(0, GL_RGB16F, 0, GL_RGB, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		mainTex = tb.endTexture();
		FramebufferBuilder fb = new FramebufferBuilder();

		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(width, height);
		fb.framebufferTexture(GL_COLOR_ATTACHMENT0, mainTex, 0);
		main = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		main.dispose();
		mainTex.dispose();
	}

	public Texture getMainTex() {
		return mainTex;
	}

	@Override
	public void addLight(DirectionalLightInternal l) {
		if (l == null)
			return;
		l.init();
		lights.add(l);
	}

	@Override
	public void removeLight(DirectionalLightInternal l) {
		if (l == null)
			return;
		synchronized (lights) {
			lights.remove(l);
		}
		l.dispose();
	}

	public List<DirectionalLightInternal> getLights() {
		return lights;
	}

}
