/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.renderers;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.glDrawArrays;

import java.util.List;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaValue;

import engine.gl.MaterialGL;
import engine.gl.Resources;
import engine.gl.mesh.animation.AnimatedModel;
import engine.gl.mesh.animation.AnimatedModelSubMesh;
import engine.glv2.entities.LayeredCubeCamera;
import engine.glv2.renderers.shaders.AnimInstanceBaseShadowShader;
import engine.glv2.renderers.shaders.AnimInstanceDirectionalShadowShader;
import engine.glv2.renderers.shaders.AnimInstancePointShadowShader;
import engine.glv2.renderers.shaders.AnimInstanceSpotShadowShader;
import engine.glv2.v2.lights.DirectionalLightCamera;
import engine.glv2.v2.lights.SpotLightCamera;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.animation.AnimationController;

public class AnimInstanceShadowRenderer {

	private AnimInstanceDirectionalShadowShader directionalShader;
	private AnimInstanceSpotShadowShader spotShader;
	private AnimInstancePointShadowShader pointShader;

	// TODO: this should NOT be here
	private static final LuaValue C_ANIMATIONCONTROLLER = LuaValue.valueOf("AnimationController");

	public AnimInstanceShadowRenderer() {
		directionalShader = new AnimInstanceDirectionalShadowShader();
		directionalShader.init();
		spotShader = new AnimInstanceSpotShadowShader();
		spotShader.init();
		pointShader = new AnimInstancePointShadowShader();
		pointShader.init();
	}

	protected void renderShadow(List<Instance> instances, DirectionalLightCamera camera) {
		directionalShader.start();
		directionalShader.loadDirectionalLight(camera);
		for (Instance instance : instances) {
			renderInstance(instance, directionalShader);
		}
		directionalShader.stop();
	}

	protected void renderShadow(List<Instance> instances, SpotLightCamera camera) {
		spotShader.start();
		spotShader.loadSpotLight(camera);
		for (Instance instance : instances) {
			renderInstance(instance, spotShader);
		}
		spotShader.stop();
	}

	protected void renderShadow(List<Instance> instances, LayeredCubeCamera camera) {
		pointShader.start();
		pointShader.loadPointLight(camera);
		for (Instance instance : instances) {
			renderInstance(instance, pointShader);
		}
		pointShader.stop();
	}

	private void renderInstance(Instance inst, AnimInstanceBaseShadowShader shader) {
		AnimationController anim = (AnimationController) inst.findFirstChildOfClass(C_ANIMATIONCONTROLLER);
		if (anim == null)
			return;

		GameObject go = anim.getLinkedInstance();
		if ( go == null )
			return;
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
		for (int i = 0; i < model.getMeshes().size(); i++) {
			AnimatedModelSubMesh mesh = model.getMeshes().get(i);

			MaterialGL material = Resources.MATERIAL_BLANK;
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

			mesh.bind();
			glDrawArrays(GL_TRIANGLES, 0, mesh.size());
			mesh.unbind();
		}
	}

	public void dispose() {
		directionalShader.dispose();
		spotShader.dispose();
		pointShader.dispose();
	}

}
