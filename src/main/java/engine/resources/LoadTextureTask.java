/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.resources;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_info_from_memory;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

import engine.gl.exceptions.DecodeTextureException;
import engine.gl.objects.RawTexture;
import engine.gl.objects.Texture;
import engine.tasks.Task;

public class LoadTextureTask extends Task<Texture> {

	private String file;
	private int filter;
	private int textureWarp;
	private int format;
	private boolean textureMipMapAF, flipY;

	public LoadTextureTask(String file, int filter, int textureWarp, int format, boolean textureMipMapAF,
			boolean flipY) {
		this.file = file;
		this.filter = filter;
		this.textureWarp = textureWarp;
		this.format = format;
		this.textureMipMapAF = textureMipMapAF;
		this.flipY = flipY;
	}

	@Override
	protected Texture call() {
		System.out.println("Loading: " + file);
		RawTexture data = decodeTextureFile(file, flipY);
		int id = ResourcesManager.backend.loadTexture(filter, textureWarp, format, GL_UNSIGNED_BYTE, textureMipMapAF,
				data);
		data.dispose();
		return new Texture(id, GL_TEXTURE_2D, data.getWidth(), data.getHeight());
	}

	private RawTexture decodeTextureFile(String file, boolean flipY) {
		ByteBuffer imageBuffer;
		try {
			imageBuffer = ResourcesManager.ioResourceToByteBuffer(file, 1024 * 1024);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		int width = 0;
		int height = 0;
		int component = 0;
		ByteBuffer image;
		try (MemoryStack stack = stackPush()) {
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			IntBuffer comp = stack.mallocInt(1);

			if (!stbi_info_from_memory(imageBuffer, w, h, comp))
				throw new DecodeTextureException("Failed to read image information: " + stbi_failure_reason());

			// System.out.println("Image width: " + w.get(0) + "\nImage height: " + h.get(0)
			// + "\nImage components: "
			// + comp.get(0) + "\nImage HDR: " + stbi_is_hdr_from_memory(imageBuffer));
			stbi_set_flip_vertically_on_load(flipY);

			image = stbi_load_from_memory(imageBuffer, w, h, comp, 0);
			memFree(imageBuffer);

			if (image == null)
				throw new DecodeTextureException("Failed to load image: " + stbi_failure_reason());
			width = w.get(0);
			height = h.get(0);
			component = comp.get(0);
		}
		return new RawTexture(image, width, height, component);
	}

}
