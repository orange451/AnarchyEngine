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

import engine.gl.objects.Texture;
import engine.gl.objects.TextureBuilder;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL43C.*;

public class VoxelizedManager {

	private Texture tex;

	public VoxelizedManager() {
		init();
	}

	public void init() {
		TextureBuilder tb = new TextureBuilder();
		tb.genTexture(GL_TEXTURE_3D).bindTexture();
		tb.sizeTexture(256, 256, 256).texImage3D(0, GL_RGBA16F, 0, GL_RGBA, GL_FLOAT, 0);
		tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		tb.texParameteri(GL_TEXTURE_BASE_LEVEL, 0);
		tb.texParameteri(GL_TEXTURE_MAX_LEVEL, 0);
		tb.texParameteri(GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
		tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
		tex = tb.endTexture();
	}

	public void dispose() {
		tex.dispose();
	}
	
	public Texture getTexture() {
		return tex;
	}

}
