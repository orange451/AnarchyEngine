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
import static org.lwjgl.opengl.GL13C.glActiveTexture;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import engine.gl.IObjectRenderer;
import engine.gl.IRenderingData;
import engine.gl.RendererData;
import engine.gl.Resources;
import engine.gl.entities.LayeredCubeCamera;
import engine.gl.lights.DirectionalLightCamera;
import engine.gl.lights.SpotLightCamera;
import engine.gl.mesh.BufferedMesh;
import engine.gl.objects.MaterialGL;
import engine.gl.renderers.shaders.InstanceDeferredShader;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;

public class InstanceRenderer implements IObjectRenderer {

	public static final int ENTITY_RENDERER_ID = 1;

	private InstanceDeferredShader shader;
	private List<Instance> instances = new ArrayList<>();
	private InstanceShadowRenderer shadowRenderer;
	private InstanceForwardRenderer forwardRenderer;
	private InstanceCubeRenderer cubeRenderer;

	private Matrix4f temp = new Matrix4f();

	public InstanceRenderer() {
		shader = new InstanceDeferredShader();
		shader.init();
		shadowRenderer = new InstanceShadowRenderer();
		forwardRenderer = new InstanceForwardRenderer();
		cubeRenderer = new InstanceCubeRenderer();
	}

	@Override
	public void preProcess(List<Instance> instances) {
		for (Instance instance : instances) {
			processInstance(instance);
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
	public void renderReflections(IRenderingData rd, RendererData rnd, LayeredCubeCamera cubeCamera) {
		cubeRenderer.render(instances, rnd, cubeCamera);
	}

	@Override
	public void renderForward(IRenderingData rd, RendererData rnd) {
		forwardRenderer.render(instances, rd, rnd);
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
	public void renderShadow(LayeredCubeCamera camera) {
		shadowRenderer.renderShadow(instances, camera);
	}

	@Override
	public void end() {
		instances.clear();
	}

	private void processInstance(Instance inst) {
		GameObject go = (GameObject) inst;
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;
		if (go.getPrefab() == null)
			return;
		instances.add(inst);
	}

	private void renderInstance(Instance inst) {
		GameObject go = (GameObject) inst;
		if (go.isDestroyed())
			return;
		if (go.getParent().isnil())
			return;

		Prefab goPrefab = go.getPrefab();
		if (goPrefab == null)
			return;

		PrefabRenderer pfr = goPrefab.getPrefab();
		if (pfr == null)
			return;

		Matrix4f mat = go.getWorldMatrix().toJoml();
		if ( goPrefab.isCenterOrigin() )
			mat.translate(pfr.getAABBOffset());
		mat.scale(pfr.getParent().getScale());
		shader.loadTransformationMatrix(mat);

		Matrix4f prevMat = temp.set(go.getPreviousWorldMatrixJOML());
		prevMat.translate(pfr.getAABBOffset());
		prevMat.scale(pfr.getParent().getScale());
		shader.loadTransformationMatrixPrev(prevMat);
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
		cubeRenderer.dispose();
	}

	@Override
	public int getID() {
		return ENTITY_RENDERER_ID;
	}

}
