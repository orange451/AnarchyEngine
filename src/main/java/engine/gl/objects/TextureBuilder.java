/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.objects;

import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glGenTextures;
import static org.lwjgl.opengl.GL11C.glTexImage2D;
import static org.lwjgl.opengl.GL11C.glTexParameterf;
import static org.lwjgl.opengl.GL11C.glTexParameterfv;
import static org.lwjgl.opengl.GL11C.glTexParameteri;
import static org.lwjgl.opengl.GL12C.glTexImage3D;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public class TextureBuilder {

	private int texture, target;
	private boolean working;
	private int width, height, depth;

	public TextureBuilder() {
	}

	public TextureBuilder genTexture(int target) {
		if (working)
			throw new IllegalStateException("Already working on a Texture.");
		working = true;
		texture = glGenTextures();
		this.target = target;
		return this;
	}

	public TextureBuilder bindTexture() {
		check();
		glBindTexture(target, texture);
		return this;
	}

	public TextureBuilder sizeTexture(int width, int height) {
		check();
		this.width = width;
		this.height = height;
		return this;
	}

	public TextureBuilder sizeTexture(int width, int height, int depth) {
		check();
		this.width = width;
		this.height = height;
		this.depth = depth;
		return this;
	}

	public TextureBuilder texImage2D(int level, int internalformat, int border, int format, int type, int pixels) {
		check();
		glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
		return this;
	}

	public TextureBuilder texImage2D(int target, int level, int internalformat, int border, int format, int type,
			int pixels) {
		check();
		glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
		return this;
	}

	public TextureBuilder texImage3D(int level, int internalformat, int border, int format, int type, int pixel) {
		check();
		glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixel);
		return this;
	}

	public TextureBuilder texParameteri(int pname, int param) {
		check();
		glTexParameteri(target, pname, param);
		return this;
	}

	public TextureBuilder texParameterf(int pname, float param) {
		check();
		glTexParameterf(target, pname, param);
		return this;
	}

	public TextureBuilder texParameterfv(int pname, float[] params) {
		check();
		glTexParameterfv(target, pname, params);
		return this;
	}

	public TextureBuilder generateMipmap() {
		check();
		glGenerateMipmap(target);
		return this;
	}

	public Texture endTexture() {
		check();
		glBindTexture(target, 0);
		working = false;
		return new Texture(texture, target, width, height, depth);
	}

	private void check() throws IllegalStateException {
		if (!working)
			throw new IllegalStateException("Not working on a Texture.");
	}

}
