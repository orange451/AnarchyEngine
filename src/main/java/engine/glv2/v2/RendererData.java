/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.v2;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.objects.Texture;
import engine.glv2.v2.lights.DirectionalLightHandler;
import engine.glv2.v2.lights.PointLightHandler;
import engine.glv2.v2.lights.SpotLightHandler;

public class RendererData {

	public Texture irradianceCapture, environmentMap;
	public Texture brdfLUT;
	public float exposure;
	public float gamma;
	public float saturation;
	public Vector3f ambient = new Vector3f();
	public Matrix4f previousViewMatrix = new Matrix4f();
	public Matrix4f previousProjectionMatrix = new Matrix4f();
	public PointLightHandler plh;
	public DirectionalLightHandler dlh;
	public SpotLightHandler slh;
	public RenderingSettings rs;

}
