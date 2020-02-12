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

import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.system.MemoryUtil.memAlloc;

import java.nio.ByteBuffer;

import lwjgui.paint.Color;

public class RawTexture {
	private int width;
	private int height;
	private int comp;
	protected ByteBuffer buffer;

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
	
	/**
	 * Generate a Raw Texture from a LWJGUI Color. Will always be a 4 component image (RGBA).
	 * @param color
	 * @param width
	 * @param height
	 * @return
	 */
	public static RawTexture fromColor(Color color, int width, int height) {
		int comp = 4; // R G B A
		ByteBuffer buffer = memAlloc(width * height * comp);
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				buffer.put((byte)(color.getRed() & 0xff));
				buffer.put((byte)(color.getGreen() & 0xff));
				buffer.put((byte)(color.getBlue() & 0xff));
				buffer.put((byte)(color.getAlpha() & 0xff));
			}
		}
		
		buffer.flip();
		return new RawTexture(buffer, width, height, comp);
	}
}
