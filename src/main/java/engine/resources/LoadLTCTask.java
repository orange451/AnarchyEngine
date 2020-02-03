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

import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_REPEAT;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_info_from_memory;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

import engine.gl.exceptions.DecodeTextureException;
import engine.gl.objects.RawLTC;
import engine.gl.objects.Texture;
import engine.tasks.Task;

public class LoadLTCTask extends Task<Texture> {

	private static final int READ_HEADER = 0;
	private static final int READ_VERSION = 1;
	private static final int READ_TYPE = 2;
	private static final int READ_SIZE = 3;
	private static final int READ_DATA = 4;
	private static final int READ_DONE = 5;

	private String file;
	private int size;

	public LoadLTCTask(String file, int size) {
		this.file = file;
		this.size = size;
	}

	@Override
	protected Texture call() {
		System.out.println("Loading: " + file);
		RawLTC data = decodeTextureFile(file);
		int id = ResourcesManager.backend.loadTexture(GL_LINEAR, GL_REPEAT, GL_RGBA32F, GL_FLOAT, false, data);
		data.dispose();
		return new Texture(id, GL_TEXTURE_2D, data.getWidth(), data.getHeight());
	}

	private RawLTC decodeTextureFile(String file) {
		ByteBuffer ltc;
		try {
			ltc = ResourcesManager.ioResourceToByteBuffer(file, 1024 * 1024);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		int version = -1, type = -1, bufferSize = -1;
		ByteBuffer image = null;
		int status = READ_HEADER;
		LOOP: while (ltc.hasRemaining()) {
			switch (status) {
			case READ_HEADER: // Read Header
				byte[] formatBytes = new byte[3];
				ltc.get(formatBytes);
				String format = new String(formatBytes);
				if (!format.equals("LTC"))
					throw new DecodeTextureException("Invalid Format");
				System.out.println(format);
				status = READ_VERSION;
				break;
			case READ_VERSION:
				version = ltc.getInt();
				System.out.println("Version: " + version);
				status = READ_TYPE;
				break;
			case READ_TYPE:
				type = ltc.getInt();
				System.out.println("Type: " + type);
				status = READ_SIZE;
				break;
			case READ_SIZE:
				bufferSize = ltc.getInt();
				System.out.println("Size: " + bufferSize);
				status = READ_DATA;
				break;
			case READ_DATA:
				image = memAlloc(bufferSize * 4 + 1);
				memCopy(ltc, image);
				status = READ_DONE;
				break;
			case READ_DONE:
				break LOOP;
			}
		}
		memFree(ltc);
		
		return new RawLTC(image, size, size, type == 1 ? 2 : 4);
	}

}
