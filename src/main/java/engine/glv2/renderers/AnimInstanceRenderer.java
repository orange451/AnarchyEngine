package engine.glv2.renderers;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glGetIntegerv;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.luaj.vm2.LuaValue;

import engine.gl.MaterialGL;
import engine.gl.Resources;
import engine.gl.mesh.animation.AnimatedModel;
import engine.gl.mesh.animation.AnimatedModelSubMesh;
import engine.glv2.entities.CubeMapCamera;
import engine.glv2.renderers.shaders.AnimInstanceDeferredShader;
import engine.glv2.v2.IObjectRenderer;
import engine.glv2.v2.IRenderingData;
import engine.glv2.v2.RendererData;
import engine.glv2.v2.lights.DirectionalLightCamera;
import engine.glv2.v2.lights.SpotLightCamera;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.AnimationController;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;

public class AnimInstanceRenderer implements IObjectRenderer {

	public static final int ANIMATED_INSTANCE_RENDERER = 2;

	private AnimInstanceDeferredShader shader;
	private List<Instance> instances = new ArrayList<>();
	private AnimInstanceShadowRenderer shadowRenderer;
	private AnimInstanceForwardRenderer forwardRenderer;

	// TODO: Temporary res storage
	private int[] viewport = new int[4];
	private Vector2f resolution = new Vector2f();

	// TODO: this should NOT be here
	private static final LuaValue C_ANIMATIONCONTROLLER = LuaValue.valueOf("AnimationController");

	public AnimInstanceRenderer() {
		shader = new AnimInstanceDeferredShader();
		shadowRenderer = new AnimInstanceShadowRenderer();
		forwardRenderer = new AnimInstanceForwardRenderer();
	}

	@Override
	public void preProcess(List<Instance> instances) {
		for (Instance entity : instances) {
			processEntity(entity);
		}
	}

	@Override
	public void render(IRenderingData rd, RendererData rnd) {
		glGetIntegerv(GL_VIEWPORT, viewport);
		resolution.set(viewport[2], viewport[3]);
		shader.start();
		shader.loadCamera(rd.camera, rd.projectionMatrix, resolution, rnd.rs.taaEnabled);
		for (Instance instance : instances) {
			renderInstance(instance);
		}
		shader.stop();
	}

	@Override
	public void renderReflections(IRenderingData rd, RendererData rnd, CubeMapCamera cubeCamera) {
		forwardRenderer.render(instances, rd, rnd, cubeCamera, false/* ,MaterialType.OPAQUE */, false);
	}

	@Override
	public void renderForward(IRenderingData rd, RendererData rnd) {
		forwardRenderer.render(instances, rd, rnd, null, true/* ,MaterialType.TRANSPARENT */, true);
	}

	@Override
	public void renderShadow(DirectionalLightCamera camera) {
		shadowRenderer.renderShadow(instances, camera);
	}

	@Override
	public void renderShadow(SpotLightCamera camera) {
		shadowRenderer.renderShadow(instances, camera);
	}

	@Override
	public void end() {
		instances.clear();
	}

	private void processEntity(Instance instance) {
		AnimationController anim = (AnimationController) instance.findFirstChildOfClass(C_ANIMATIONCONTROLLER);
		anim.getAnimatedModel().renderV2();
		instances.add(instance);
	}

	private void renderInstance(Instance inst) {
		AnimationController anim = (AnimationController) inst.findFirstChildOfClass(C_ANIMATIONCONTROLLER);

		GameObject go = anim.getLinkedInstance();
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab() == null)
			return;
		AnimatedModel model = anim.getAnimatedModel();

		Matrix4f mat = go.getWorldMatrix().toJoml();
		mat.scale(go.getPrefab().getScale());
		shader.loadTransformationMatrix(mat);
		shader.loadBoneMat(model.getBoneBuffer());
		for (int i = 0; i < model.getMeshes().size(); i++) {
			AnimatedModelSubMesh mesh = model.getMeshes().get(i);

			engine.gl.MaterialGL material = Resources.MATERIAL_BLANK;
			Material ECSMat = model.getMeshToModelMap().get(mesh).getMaterial();
			if (ECSMat != null) {
				MaterialGL GLMat = ECSMat.getMaterial();
				if (GLMat != null) {
					material = GLMat;
				}
			}
			float iMatTrans = 1.0f - material.getTransparency();
			float iObjTrans = 1.0f - go.getTransparency();
			float trans = iMatTrans * iObjTrans;
			if (trans != 1.0)
				continue;

			prepareMaterial(material);
			shader.loadMaterial(material);
			mesh.bind();
			glDrawArrays(GL_TRIANGLES, 0, mesh.size());
			mesh.unbind();
		}
	}

	private void prepareMaterial(engine.gl.MaterialGL mat) {
		if (mat.getDiffuseTexture().getID() != -1) {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, mat.getDiffuseTexture().getID());
		}
		if (mat.getNormalTexture().getID() != -1) {
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, mat.getNormalTexture().getID());
		}
		if (mat.getMetalnessTexture().getID() != -1) {
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, mat.getMetalnessTexture().getID());
		}
		if (mat.getRoughnessTexture().getID() != -1) {
			glActiveTexture(GL_TEXTURE3);
			glBindTexture(GL_TEXTURE_2D, mat.getRoughnessTexture().getID());
		}
	}

	@Override
	public void dispose() {
		shader.dispose();
		shadowRenderer.dispose();
		forwardRenderer.dispose();
	}

	@Override
	public int getID() {
		return ANIMATED_INSTANCE_RENDERER;
	}

}
