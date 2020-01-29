/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;
import org.luaj.vm2.LuaValue;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntMap.Entry;
import com.esotericsoftware.kryonet.util.ObjectIntMap;

import engine.gl.entities.LayeredCubeCamera;
import engine.gl.lights.DirectionalLightCamera;
import engine.gl.lights.SpotLightCamera;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.animation.AnimationController;
import engine.observer.RenderableInstance;

public class RenderingManager {

	// TODO: this should NOT be here
	private static final LuaValue C_ANIMATIONCONTROLLER = LuaValue.valueOf("AnimationController");

	private final IntMap<IObjectRenderer> objectRenderers = new IntMap<>();
	private final ObjectIntMap<List<Instance>> entitiesToRenderers = new ObjectIntMap<>();

	public RenderingManager() {
	}

	public void addRenderer(IObjectRenderer objectRenderer) {
		objectRenderers.put(objectRenderer.getID(), objectRenderer);
	}

	public void preProcess(Instance world) {
		List<Instance> instances = world.getDescendantsUnsafe();
		synchronized (instances) {
			for (Instance entity : instances)
				process(entity);
		}
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers) {
			IObjectRenderer objectRenderer = rendererEntry.value;
			List<Instance> batch = entitiesToRenderers.findKey(objectRenderer.getID());
			if (batch != null)
				objectRenderer.preProcess(batch);
		}
	}

	public void render(IRenderingData rd, RendererData rnd, Vector2f resolution) {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.render(rd, rnd, resolution);
	}

	public void renderReflections(IRenderingData rd, RendererData rnd, LayeredCubeCamera cubeCamera) {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.renderReflections(rd, rnd, cubeCamera);
	}

	public void renderForward(IRenderingData rd, RendererData rnd) {
		glEnable(GL_BLEND);
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.renderForward(rd, rnd);
		glDisable(GL_BLEND);
	}

	public void renderShadow(DirectionalLightCamera camera) {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.renderShadow(camera);
	}

	public void renderShadow(SpotLightCamera camera) {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.renderShadow(camera);
	}

	public void renderShadow(LayeredCubeCamera camera) {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.renderShadow(camera);
	}

	public void end() {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers) {
			IObjectRenderer objectRenderer = rendererEntry.value;
			List<Instance> batch = entitiesToRenderers.findKey(objectRenderer.getID());
			if (batch != null)
				batch.clear();
			objectRenderer.end();
		}
	}

	private void process(Instance root) {
		if (root instanceof RenderableInstance) {
			Instance animationController = root.findFirstChildOfClass(C_ANIMATIONCONTROLLER);
			boolean hasAnimationController = animationController != null
					&& ((AnimationController) animationController).getPlayingAnimations() > 0;
			int id = hasAnimationController ? 2 : 1; // TODO: Poll current instance
														// renderer id
			List<Instance> batch = entitiesToRenderers.findKey(id);
			if (batch != null)
				batch.add(root);
			else {
				List<Instance> newBatch = new ArrayList<>();
				newBatch.add(root);
				entitiesToRenderers.put(newBatch, id);
			}
		}
	}

	public void dispose() {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.dispose();
	}

}
