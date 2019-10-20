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

package engine.glv2.objects;

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
		return new Texture(texture, target, width, height);
	}

	private void check() throws IllegalStateException {
		if (!working)
			throw new IllegalStateException("Not working on a Texture.");
	}

}
