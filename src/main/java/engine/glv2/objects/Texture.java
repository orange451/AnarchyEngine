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
import static org.lwjgl.opengl.GL11C.glDeleteTextures;

public class Texture implements IVisualObject {

	private final int texture, target;
	private final int width, height;

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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Texture) {
			Texture t = (Texture) obj;
			return t.getTexture() == texture;
		}
		return false;
	}

}
