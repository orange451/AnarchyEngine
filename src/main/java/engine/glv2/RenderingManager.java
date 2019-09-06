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

package engine.glv2;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntMap.Entry;
import com.esotericsoftware.kryonet.util.ObjectIntMap;

import engine.glv2.entities.CubeMapCamera;
import engine.glv2.entities.SunCamera;
import engine.glv2.v2.IRenderingData;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Camera;
import engine.observer.RenderableInstance;

public class RenderingManager {

	private final IntMap<IObjectRenderer> objectRenderers = new IntMap<>();
	private final ObjectIntMap<List<Instance>> entitiesToRenderers = new ObjectIntMap<>();

	public RenderingManager() {
	}

	public void addRenderer(IObjectRenderer objectRenderer) {
		objectRenderers.put(objectRenderer.getID(), objectRenderer);
	}

	public void preProcess(Instance world) {
		List<Instance> entities = world.getChildren();
		for (Instance entity : entities)
			process(entity);
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers) {
			IObjectRenderer objectRenderer = rendererEntry.value;
			List<Instance> batch = entitiesToRenderers.findKey(objectRenderer.getID());
			if (batch != null)
				objectRenderer.preProcess(batch);
		}
	}

	public void render(Camera camera, Matrix4f projection) {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.render(camera, projection);
	}

	public void renderReflections(IRenderingData rd, RendererData rnd, CubeMapCamera cubeCamera) {
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.renderReflections(rd, rnd, cubeCamera);
	}

	public void renderForward(IRenderingData rd, RendererData rnd) {
		glEnable(GL_BLEND);
		for (Entry<IObjectRenderer> rendererEntry : objectRenderers)
			rendererEntry.value.renderForward(rd, rnd);
		glDisable(GL_BLEND);
	}

	public void renderShadow(SunCamera camera) {
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
		for (Instance inst : root.getChildren()) {
			if (inst instanceof Camera)
				continue;
			process(inst);
		}
		if (root instanceof RenderableInstance) {
			int id = 1; // TODO: Poll current instance renderer id
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
