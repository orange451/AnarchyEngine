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

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL30C.GL_TEXTURE_2D_ARRAY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.RendererData;
import engine.gl.Resources;
import engine.gl.entities.LayeredCubeCamera;
import engine.gl.lights.PointLightInternal;
import engine.gl.mesh.BufferedMesh;
import engine.gl.objects.MaterialGL;
import engine.gl.renderers.shaders.InstanceCubeShader;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;

public class InstanceCubeRenderer {

	private InstanceCubeShader shader;

	public InstanceCubeRenderer() {
		shader = new InstanceCubeShader();
		shader.init();
	}

	public void render(List<Instance> instances, RendererData rnd, LayeredCubeCamera cubeCamera) {
		shader.start();
		shader.loadCamera(cubeCamera);
		shader.colorCorrect(false);
		shader.loadSettings(rnd.rs.shadowsEnabled);
		shader.loadExposure(rnd.exposure);
		shader.loadGamma(rnd.gamma);
		shader.loadDirectionalLights(rnd.dlh.getLights());
		glActiveTexture(GL_TEXTURE4);
		glBindTexture(GL_TEXTURE_CUBE_MAP, rnd.irradianceCapture.getTexture());
		glActiveTexture(GL_TEXTURE5);
		glBindTexture(GL_TEXTURE_CUBE_MAP, rnd.environmentMap.getTexture());
		glActiveTexture(GL_TEXTURE6);
		glBindTexture(GL_TEXTURE_2D, rnd.brdfLUT.getTexture());
		for(int x = 0; x < 4; x++) {
			glActiveTexture(GL_TEXTURE8 + x);
			glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
		}
		for(int x = 0; x < 4; x++) {
			glActiveTexture(GL_TEXTURE12 + x);
			glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
		}
		for (int x = 0; x < Math.min(4, rnd.dlh.getLights().size()); x++) {
			glActiveTexture(GL_TEXTURE8 + x);
			glBindTexture(GL_TEXTURE_2D_ARRAY, rnd.dlh.getLights().get(x).getShadowMap().getShadowMaps().getTexture());
		}
		for (int x = 0; x < Math.min(4, rnd.plh.getLights().size()); x++) {
			glActiveTexture(GL_TEXTURE12 + x);
			glBindTexture(GL_TEXTURE_CUBE_MAP, rnd.plh.getLights().get(x).getShadowMap().getTexture().getTexture());
		}
		for (Instance instance : instances) {
			renderInstance(instance, rnd);
		}
		shader.stop();
	}

	private void renderInstance(Instance inst, RendererData rnd) {
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

		Vector3f gop = go.getPosition().toJoml();

		List<PointLightInternal> pl = new ArrayList<>();
		synchronized (rnd.plh.getLights()) {
			for (PointLightInternal p : rnd.plh.getLights()) {
				if (p.getPosition().distance(gop) < p.radius)
					pl.add(p);
			}
		}
		pl = pl.subList(0, Math.min(8, pl.size()));
		Collections.sort(pl, new Comparator<PointLightInternal>() {
			@Override
			public int compare(PointLightInternal o1, PointLightInternal o2) {
				float d1 = o1.getPosition().distanceSquared(gop);
				float d2 = o1.getPosition().distanceSquared(gop);
				return (int) (d2 - d1);
			}
		});

		shader.loadPointLights(pl);
		for (int i = 0; i < pfr.size(); i++) {
			Model p = pfr.getModel(i);
			BufferedMesh m = p.getMeshInternal();

			if (m == null)
				m = Resources.MESH_SPHERE;

			engine.gl.objects.MaterialGL material = Resources.MATERIAL_BLANK;
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

			prepareMaterial(material);
			shader.loadMaterial(material);
			shader.loadTransparency(trans);
			m.bind();
			m.render();
			m.unbind();
		}
	}

	private void prepareMaterial(engine.gl.objects.MaterialGL mat) {
		mat.getDiffuseTexture().active(GL_TEXTURE0);
		mat.getNormalTexture().active(GL_TEXTURE1);
		mat.getMetalnessTexture().active(GL_TEXTURE2);
		mat.getRoughnessTexture().active(GL_TEXTURE3);
	}

	public void dispose() {
		shader.dispose();
	}

}
