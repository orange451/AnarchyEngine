/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;

import engine.gl.MaterialGL;
import engine.gl.shader.BaseShader;
import engine.util.Pair;

public class BufferedPrefab {
	private List<Pair<BufferedMesh,MaterialGL>> models;
	
	public BufferedPrefab() {
		models = Collections.synchronizedList(new ArrayList<Pair<BufferedMesh,MaterialGL>>());
	}

	public void render(BaseShader shader, Matrix4f worldMatrix) {
		
		// World matrix remains the same for all meshes inside. So you can optimize by setting once.
		shader.setWorldMatrix(worldMatrix);
		
		// Loop through each model and render
		synchronized(models) {
			for (int i = 0; i < models.size(); i++) {
				Pair<BufferedMesh, MaterialGL> p = models.get(i);
				BufferedMesh mesh = p.value1();
				engine.gl.MaterialGL material = p.value2();
				
				mesh.render(shader, null, material);
			}
		}
	}

	public void addModel(BufferedMesh mesh, MaterialGL material) {
		synchronized(models) {
			models.add(new Pair<BufferedMesh, MaterialGL>(mesh, material));
		}
	}

	public BufferedMesh collapseModel() {
		BufferedMesh[] meshes = new BufferedMesh[models.size()];
		for (int i = 0; i < meshes.length; i++) {
			meshes[i] = models.get(i).value1();
		}
		return BufferedMesh.combineMeshes(meshes);
	}
}
