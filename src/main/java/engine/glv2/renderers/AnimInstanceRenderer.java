package engine.glv2.renderers;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
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
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.AnimationController;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;

public class AnimInstanceRenderer implements IObjectRenderer {

	public static final int ANIMATED_INSTANCE_RENDERER = 2;

	private AnimInstanceDeferredShader shader;
	private List<Instance> instances = new ArrayList<>();
	private AnimInstanceShadowRenderer shadowRenderer;
	private AnimInstanceForwardRenderer forwardRenderer;

	// TODO: this should NOT be here
	private static final LuaValue C_ANIMATIONCONTROLLER = LuaValue.valueOf("AnimationController");

	private Matrix4f temp = new Matrix4f();

	public AnimInstanceRenderer() {
		shader = new AnimInstanceDeferredShader();
		shader.init();
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
	public void render(IRenderingData rd, RendererData rnd, Vector2f resolution) {
		shader.start();
		shader.loadCamera(rd.camera, rd.projectionMatrix, resolution, rnd.rs.taaEnabled);
		shader.loadCameraPrev(rnd.previousViewMatrix, rnd.previousProjectionMatrix);
		for (Instance instance : instances) {
			renderInstance(instance);
		}
		shader.stop();
	}

	@Override
	public void renderReflections(IRenderingData rd, RendererData rnd, CubeMapCamera cubeCamera) {
		forwardRenderer.render(instances, rd, rnd, cubeCamera, false, false);
	}

	@Override
	public void renderForward(IRenderingData rd, RendererData rnd) {
		forwardRenderer.render(instances, rd, rnd, null, true, true);
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
		AnimatedModel animatedModel = anim.getAnimatedModel();
		if ( animatedModel == null )
			return;
		
		anim.getAnimatedModel().renderV2();
		instances.add(instance);
	}

	private void renderInstance(Instance inst) {
		AnimationController anim = (AnimationController) inst.findFirstChildOfClass(C_ANIMATIONCONTROLLER);
		if ( anim == null )
			return;
		
		GameObject go = anim.getLinkedInstance();
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab() == null)
			return;
		AnimatedModel model = anim.getAnimatedModel();
		PrefabRenderer pfr = go.getPrefab().getPrefab();

		Matrix4f mat = go.getWorldMatrix().toJoml();
		mat.translate(pfr.getAABBOffset());
		mat.scale(go.getPrefab().getScale());
		shader.loadTransformationMatrix(mat);
		shader.loadBoneMat(model.getBoneBuffer());

		Matrix4f prevMat = temp.set(go.getPreviousWorldMatrixJOML());
		prevMat.scale(go.getPrefab().getScale());
		shader.loadTransformationMatrixPrev(prevMat);
		shader.loadBoneMatPrev(model.getPreviousBoneBuffer());
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
