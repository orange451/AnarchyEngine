/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.renderers;

import java.util.List;

import org.joml.Matrix4f;

import engine.gl.MaterialGL;
import engine.gl.Resources;
import engine.gl.mesh.BufferedMesh;
import engine.glv2.renderers.shaders.InstanceBaseShadowShader;
import engine.glv2.renderers.shaders.InstanceDirectionalShadowShader;
import engine.glv2.renderers.shaders.InstanceSpotShadowShader;
import engine.glv2.v2.lights.DirectionalLightCamera;
import engine.glv2.v2.lights.SpotLightCamera;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Model;

public class InstanceShadowRenderer {

	private InstanceDirectionalShadowShader directionalShader;
	private InstanceSpotShadowShader spotShader;

	public InstanceShadowRenderer() {
		directionalShader = new InstanceDirectionalShadowShader();
		directionalShader.init();
		spotShader = new InstanceSpotShadowShader();
		spotShader.init();
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

	private void renderInstance(Instance inst, InstanceBaseShadowShader shader) {
		GameObject go = (GameObject) inst;
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab() == null)
			return;
		PrefabRenderer pfr = go.getPrefab().getPrefab();

		Matrix4f mat = go.getWorldMatrix().toJoml();
		mat.translate(pfr.getAABBOffset());
		mat.scale(pfr.getParent().getScale());
		shader.loadTransformationMatrix(mat);
		for (int i = 0; i < pfr.size(); i++) {
			Model p = pfr.getModel(i);
			BufferedMesh m = p.getMeshInternal();

			if (m == null)
				m = Resources.MESH_SPHERE;

			engine.gl.MaterialGL material = Resources.MATERIAL_BLANK;
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
			m.render(null, null, null);
		}
	}

	public void dispose() {
		directionalShader.dispose();
		spotShader.dispose();
	}

}
