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

import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glColorMask;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glGetIntegerv;
import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE8;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL15C.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;
import static org.lwjgl.opengl.GL30C.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL42C.glBindImageTexture;
import static org.lwjgl.opengl.GL44C.glClearTexImage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.Resources;
import engine.gl.lights.PointLightInternal;
import engine.gl.mesh.BufferedMesh;
import engine.gl.objects.MaterialGL;
import engine.gl.renderers.shaders.InstanceVoxelizeShader;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;

public class InstanceVoxelizeRenderer {
	private InstanceVoxelizeShader shader;

	public InstanceVoxelizeRenderer() {
		shader = new InstanceVoxelizeShader();
		shader.init();
		Game.waitForService(LuaValue.valueOf("UserInputService"));
		Game.userInputService().inputBeganEvent().connect((args) -> {
			if (args[0].get("KeyCode").eq_b(LuaValue.valueOf(GLFW.GLFW_KEY_F6))) {
				System.out.println("Reloading Shaders...");
				shader.reload();
			}
		});
	}

	public void render(List<Instance> instances, IRenderingData rd, RendererData rnd) {
		glClearTexImage(rnd.vm.getTexture().getTexture(), 0, GL_RGBA, GL_FLOAT, (ByteBuffer)null);
		int[] oldViewport = new int[4];
		glGetIntegerv(GL_VIEWPORT, oldViewport);
		glViewport(0, 0, rnd.vm.getResolution(), rnd.vm.getResolution());
		glDisable(GL_CULL_FACE);
		glDisable(GL_DEPTH_TEST);
		glColorMask(false, false, false, false);

		shader.start();
		shader.loadVoxelizationValues(rnd.vm, rd.camera);
		shader.loadSettings(rnd.rs.shadowsEnabled);
		shader.loadDirectionalLights(rnd.dlh.getLights());
		glBindImageTexture(4, rnd.vm.getTexture().getTexture(), 0, true, 128, GL_WRITE_ONLY, GL_RGBA16F);
		for (int x = 0; x < Math.min(8, rnd.dlh.getLights().size()); x++) {
			glActiveTexture(GL_TEXTURE8 + x);
			glBindTexture(GL_TEXTURE_2D_ARRAY, rnd.dlh.getLights().get(x).getShadowMap().getShadowMaps().getTexture());
		}
		for (Instance instance : instances) {
			renderInstance(instance, rnd);
		}
		shader.stop();
		glViewport(oldViewport[0], oldViewport[1], oldViewport[2], oldViewport[3]);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glColorMask(true, true, true, true);
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
		if (prefab.isCenterOrigin())
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
			if (trans != 1.0)
				continue;

			prepareMaterial(material);
			shader.loadMaterial(material);
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
