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

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.gl.lights.AreaLightHandler;
import engine.gl.lights.DirectionalLightHandler;
import engine.gl.lights.PointLightHandler;
import engine.gl.lights.SpotLightHandler;
import engine.gl.objects.Texture;

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
	public AreaLightHandler alh;
	public RenderingSettings rs;

}
