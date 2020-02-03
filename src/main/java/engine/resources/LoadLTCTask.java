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

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_REPEAT;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30C.GL_R32F;
import static org.lwjgl.opengl.GL30C.GL_RG32F;
import static org.lwjgl.opengl.GL30C.GL_RGB32F;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.io.IOException;
import java.nio.ByteBuffer;

import engine.gl.exceptions.DecodeTextureException;
import engine.gl.objects.RawLTC;
import engine.gl.objects.Texture;
import engine.tasks.Task;

public class LoadLTCTask extends Task<Texture> {

	private static final int READ_HEADER = 0;
	private static final int READ_VERSION = 1;
	private static final int READ_TYPE = 2;
	private static final int READ_TEXUTRE_SIZE = 3;
	private static final int READ_TEXUTRE_COMPONENTS = 4;
	private static final int READ_DATA_SIZE = 5;
	private static final int READ_DATA = 6;
	private static final int READ_DONE = 7;

	private static final int VERSION = 2;

	private String file;

	private int textureComponents;

	public LoadLTCTask(String file) {
		this.file = file;
	}

	@Override
	protected Texture call() {
		System.out.println("Loading: " + file);
		RawLTC data = decodeTextureFile(file);
		int glFormat = -1;
		switch (textureComponents) {
		case 1:
			glFormat = GL_R32F;
			break;
		case 2:
			glFormat = GL_RG32F;
			break;
		case 3:
			glFormat = GL_RGB32F;
			break;
		case 4:
			glFormat = GL_RGBA32F;
			break;
		}

		int id = ResourcesManager.backend.loadTexture(GL_LINEAR, GL_REPEAT, glFormat, GL_FLOAT, false, data);
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
		int version = -1, type = -1, dataSize = -1, textureSize = -1;
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
				if (version != VERSION)
					throw new DecodeTextureException("Incorrect Version " + version + " != " + VERSION);
				status = READ_TYPE;
				break;
			case READ_TYPE:
				type = ltc.getInt();
				System.out.println("Type: " + type);
				status = READ_TEXUTRE_SIZE;
				break;
			case READ_TEXUTRE_SIZE:
				textureSize = ltc.getInt();
				System.out.println("Texture Size: " + textureSize);
				status = READ_TEXUTRE_COMPONENTS;
				break;
			case READ_TEXUTRE_COMPONENTS:
				textureComponents = ltc.getInt();
				System.out.println("Texture Components: " + textureComponents);
				status = READ_DATA_SIZE;
				break;
			case READ_DATA_SIZE:
				dataSize = ltc.getInt();
				System.out.println("Data Size: " + dataSize);
				status = READ_DATA;
				break;
			case READ_DATA:
				image = memAlloc(dataSize * 4 + 1);
				memCopy(ltc, image);
				status = READ_DONE;
				break;
			case READ_DONE:
				break LOOP;
			}
		}
		memFree(ltc);

		return new RawLTC(image, textureSize, textureSize, textureComponents);
	}

}
