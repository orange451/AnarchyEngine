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
import static org.lwjgl.opengl.GL11C.glDeleteTextures;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public class Texture implements IVisualObject {

	private final int texture, target;
	private final int width, height;

	private boolean disposable = true;

	@Deprecated
	public Texture(int texture) {
		this.texture = texture;
		this.target = -1;
		this.width = -1;
		this.height = -1;
	}

	public Texture(int texture, int target, int width, int height) {
		this.texture = texture;
		this.target = target;
		this.width = width;
		this.height = height;
	}

	public void generateMipmaps() {
		glGenerateMipmap(target);
	}

	public void active(int textureNum) {
		glActiveTexture(textureNum);
		glBindTexture(target, texture);
	}

	@Override
	public void bind() {
		glBindTexture(target, texture);
	}

	@Override
	public void unbind() {
		glBindTexture(target, 0);
	}

	@Override
	public void dispose() {
		if (disposable)
			glDeleteTextures(texture);
	}

	public int getTexture() {
		return texture;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Deprecated
	public int getID() {
		return texture;
	}

	public Texture setDisposable(boolean disposable) {
		this.disposable = disposable;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Texture) {
			Texture t = (Texture) obj;
			return t.getTexture() == texture;
		}
		return false;
	}

}
