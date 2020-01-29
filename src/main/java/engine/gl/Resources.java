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

import static org.lwjgl.assimp.Assimp.AI_CONFIG_PP_CT_MAX_SMOOTHING_ANGLE;
import static org.lwjgl.assimp.Assimp.aiCreatePropertyStore;
import static org.lwjgl.assimp.Assimp.aiSetImportPropertyFloat;

import org.lwjgl.assimp.AIPropertyStore;

import engine.gl.mesh.BufferedMesh;
import engine.gl.objects.MaterialGL;
import engine.gl.objects.Texture;
import engine.resources.ResourcesManager;
import engine.tasks.TaskManager;
import engine.util.MeshUtils;

public class Resources {
	public static BufferedMesh MESH_SPHERE;
	public static BufferedMesh MESH_CUBE;
	public static BufferedMesh MESH_CONE;
	public static BufferedMesh MESH_CYLINDER;
	public static BufferedMesh MESH_UNIT_QUAD;
	public static MaterialGL MATERIAL_BLANK;

	public static AIPropertyStore propertyStore;

	public static Texture diffuse, diffuseMisc, normal, roughness, metallic;

	public static void init() {
		ResourcesManager.loadTexture("assets/textures/def/d.png", (value) -> diffuse = value.setDisposable(false));
		ResourcesManager.loadTextureMisc("assets/textures/def/d.png", (value) -> diffuseMisc = value.setDisposable(false));
		ResourcesManager.loadTextureMisc("assets/textures/def/d_n.png", (value) -> normal = value.setDisposable(false));
		ResourcesManager.loadTextureMisc("assets/textures/def/d_r.png", (value) -> roughness = value.setDisposable(false));
		ResourcesManager.loadTextureMisc("assets/textures/def/d_m.png", (value) -> metallic = value.setDisposable(false));
		TaskManager.addTaskRenderBackgroundThread(() -> {
			MATERIAL_BLANK = new MaterialGL().setDiffuseTexture(diffuse).setNormalTexture(normal)
					.setRoughnessTexture(roughness).setMetalnessTexture(metallic);
		});

		MESH_SPHERE = MeshUtils.sphere(1, 16);
		MESH_CUBE = MeshUtils.cube(1);
		MESH_CONE = MeshUtils.cone(1, 1, 16);
		MESH_CYLINDER = MeshUtils.cylinder(1, 1, 16);
		MESH_UNIT_QUAD = MeshUtils.quad(1, 1);

		propertyStore = aiCreatePropertyStore();
		aiSetImportPropertyFloat(propertyStore, AI_CONFIG_PP_CT_MAX_SMOOTHING_ANGLE, 30f);
	}

	public static void dispose() {
		diffuse.setDisposable(true).dispose();
		diffuseMisc.setDisposable(true).dispose();
		normal.setDisposable(true).dispose();
		roughness.setDisposable(true).dispose();
		metallic.setDisposable(true).dispose();
	}
}
