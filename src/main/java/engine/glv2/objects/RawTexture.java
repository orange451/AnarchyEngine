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

import static org.lwjgl.stb.STBImage.stbi_image_free;

import java.nio.ByteBuffer;

public class RawTexture {
	private int width;
	private int height;
	private int comp;
	private ByteBuffer buffer;

	public RawTexture(ByteBuffer buffer, int width, int height, int comp) {
		this.buffer = buffer;
		this.width = width;
		this.height = height;
		this.comp = comp;
	}

	public void dispose() {
		stbi_image_free(buffer);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getComp() {
		return comp;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}
}
