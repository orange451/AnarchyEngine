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

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13C.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL30C.GL_RGBA16F;

import org.joml.Matrix4f;

import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;

public class VoxelizedManager {

	private Texture color;

	private int resolution = 256;
	private int size = 50;
	private float cameraOffset;

	private Matrix4f projectionMatrix = new Matrix4f();

	public VoxelizedManager() {
		init();
		updateValues();
	}

	public void init() {
		TextureBuilder tb = new TextureBuilder();
		tb.genTexture(GL_TEXTURE_3D).bindTexture();
		tb.sizeTexture(resolution, resolution, resolution).texImage3D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		tb.texParameteri(GL_TEXTURE_BASE_LEVEL, 0);
		tb.texParameteri(GL_TEXTURE_MAX_LEVEL, 0);
		tb.texParameteri(GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
		color = tb.endTexture();
	}

	private void updateValues() {
		cameraOffset = size * 2.0f / resolution;
		projectionMatrix.setOrtho(-size, size, -size, size, size, -size);
	}

	public void setResolution(int resolution) {
		this.resolution = resolution;
		updateValues();
	}

	public void setSize(int size) {
		this.size = size;
		updateValues();
	}

	public void dispose() {
		color.dispose();
	}

	public Texture getColor() {
		return color;
	}

	public int getResolution() {
		return resolution;
	}

	public int getSize() {
		return size;
	}

	public float getCameraOffset() {
		return cameraOffset;
	}

	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

}
