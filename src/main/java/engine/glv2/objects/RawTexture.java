/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
