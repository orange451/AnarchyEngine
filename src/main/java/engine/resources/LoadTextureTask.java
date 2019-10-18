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

package engine.resources;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_info_from_memory;
import static org.lwjgl.stb.STBImage.stbi_is_hdr_from_memory;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

import engine.glv2.exceptions.DecodeTextureException;
import engine.glv2.objects.RawTexture;
import engine.glv2.objects.Texture;
import engine.tasks.Task;

public class LoadTextureTask extends Task<Texture> {

	private String file;
	private int filter;
	private int textureWarp;
	private int format;
	private boolean textureMipMapAF;

	public LoadTextureTask(String file, int filter, int textureWarp, int format, boolean textureMipMapAF) {
		this.file = file;
		this.filter = filter;
		this.textureWarp = textureWarp;
		this.format = format;
		this.textureMipMapAF = textureMipMapAF;
	}

	@Override
	protected Texture call() {
		System.out.println("Loading: " + file);
		RawTexture data = decodeTextureFile(file);
		int id = ResourcesManager.backend.loadTexture(filter, textureWarp, format, textureMipMapAF, data);
		data.dispose();
		return new Texture(id, GL_TEXTURE_2D, data.getWidth(), data.getHeight());
	}

	private RawTexture decodeTextureFile(String file) {
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

			//System.out.println("Image width: " + w.get(0) + "\nImage height: " + h.get(0) + "\nImage components: "
			//		+ comp.get(0) + "\nImage HDR: " + stbi_is_hdr_from_memory(imageBuffer));

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
