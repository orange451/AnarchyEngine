/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.renderers;

import java.util.List;

import org.joml.Matrix4f;

import engine.gl.Resources;
import engine.gl.entities.LayeredCubeCamera;
import engine.gl.lights.DirectionalLightCamera;
import engine.gl.lights.SpotLightCamera;
import engine.gl.mesh.BufferedMesh;
import engine.gl.objects.MaterialGL;
import engine.gl.renderers.shaders.InstanceBaseShadowShader;
import engine.gl.renderers.shaders.InstanceDirectionalShadowShader;
import engine.gl.renderers.shaders.InstancePointShadowShader;
import engine.gl.renderers.shaders.InstanceSpotShadowShader;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;

public class InstanceShadowRenderer {

	private InstanceDirectionalShadowShader directionalShader;
	private InstanceSpotShadowShader spotShader;
	private InstancePointShadowShader pointShader;

	public InstanceShadowRenderer() {
		directionalShader = new InstanceDirectionalShadowShader();
		directionalShader.init();
		spotShader = new InstanceSpotShadowShader();
		spotShader.init();
		pointShader = new InstancePointShadowShader();
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

	private void renderInstance(Instance inst, InstanceBaseShadowShader shader) {
		GameObject go = (GameObject) inst;
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab() == null)
			return;
		Prefab prefab = go.getPrefab();
		PrefabRenderer pfr = prefab.getPrefab();

		Matrix4f mat = go.getWorldMatrix().toJoml();
		if ( prefab.isCenterOrigin() )
			mat.translate(pfr.getAABBOffset());
		mat.scale(pfr.getParent().getScale());
		shader.loadTransformationMatrix(mat);
		for (int i = 0; i < pfr.size(); i++) {
			Model p = pfr.getModel(i);
			BufferedMesh m = p.getMeshInternal();

			if (m == null)
				m = Resources.MESH_SPHERE;

			MaterialGL material = Resources.MATERIAL_BLANK;
			Material ECSMat = p.getMaterial();
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
			m.bind();
			m.render();
			m.unbind();
		}
	}

	public void dispose() {
		directionalShader.dispose();
		spotShader.dispose();
		pointShader.dispose();
	}

}
