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
import engine.tasks.Task;
import engine.tasks.TaskManager;
import engine.util.MeshUtils;
import lwjgui.paint.Color;

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
		Task<Texture> diffTask, diffMiscTask, normTask, roughTask, metalTask;
		diffTask = ResourcesManager.createTexture(Color.WHITE, 2, 2);
		diffMiscTask = ResourcesManager.createTextureMisc(Color.WHITE, 2, 2);
		normTask = ResourcesManager.createTextureMisc(new Color(127, 127, 255), 2, 2);
		roughTask = ResourcesManager.createTextureMisc(Color.WHITE, 2, 2);
		metalTask = ResourcesManager.createTextureMisc(Color.WHITE, 2, 2);
		TaskManager.addTaskBackgroundThread(() -> {
			diffuse = diffTask.get().setDisposable(false);
			diffuseMisc = diffMiscTask.get().setDisposable(false);
			normal = normTask.get().setDisposable(false);
			roughness = roughTask.get().setDisposable(false);
			metallic = metalTask.get().setDisposable(false);
			System.out.println("GENERATING BLANK MATERIAL");
			System.out.println(diffuse + " / " + normal + " / " + roughness + " / " + metallic);
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
