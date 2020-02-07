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

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL21.GL_SRGB_ALPHA;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memRealloc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;

import org.lwjgl.system.MemoryUtil;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;

import engine.gl.GLResourcesManagerBackend;
import engine.gl.objects.Texture;
import engine.tasks.OnFinished;
import engine.tasks.Task;
import engine.tasks.TaskManager;
import engine.util.FileUtils;

public final class ResourcesManager {

	public static IResourcesManagerBackend backend = new GLResourcesManagerBackend();
	private static ObjectMap<Task<Texture>, Texture> textureTasks = new ObjectMap<>();

	public static void processShaderIncludes(String file) {
		// TaskManager.submitBackgroundThread(new ShaderIncludeTask(file));
	}

	public static Task<Texture> loadTextureMisc(String fileName, OnFinished<Texture> dst) {
		return loadTextureMisc(fileName, GL_LINEAR, true, false, dst);
	}

	public static Task<Texture> loadTextureMisc(String fileName, boolean flipY, OnFinished<Texture> dst) {
		return loadTextureMisc(fileName, GL_LINEAR, true, flipY, dst);
	}

	public static Task<Texture> loadTextureMisc(String fileName, int filter, boolean textureMipMapAF, boolean flipY,
			OnFinished<Texture> dst) {
		System.out.println("Texture scheduled to load: " + fileName);
		return TaskManager.submitRenderBackgroundThread(
				new LoadTextureTask(fileName, filter, GL_REPEAT, GL_RGBA, textureMipMapAF, flipY).setOnFinished(dst));
	}

	public static Task<Texture> loadTexture(String fileName, OnFinished<Texture> dst) {
		return loadTexture(fileName, GL_LINEAR, true, false, dst);
	}

	public static Task<Texture> loadTexture(String fileName, boolean flipY, OnFinished<Texture> dst) {
		return loadTexture(fileName, GL_LINEAR, true, flipY, dst);
	}

	public static Task<Texture> loadTexture(String fileName, int filter, boolean textureMipMapAF, boolean flipY,
			OnFinished<Texture> dst) {
		System.out.println("Texture scheduled to load: " + fileName);
		return TaskManager.submitRenderBackgroundThread(
				new LoadTextureTask(fileName, filter, GL_REPEAT, GL_SRGB_ALPHA, textureMipMapAF, flipY)
						.setOnFinished(dst));
	}

	public static void disposeTexture(Texture texture) {
		TaskManager.submitRenderBackgroundThread(new Task<Void>() {
			@Override
			protected Void call() {
				texture.dispose();
				return null;
			}
		});
	}

	public static void update() {
		Iterator<Entry<Task<Texture>, Texture>> iterator = textureTasks.iterator();
		while (iterator.hasNext()) {
			Entry<Task<Texture>, Texture> n = iterator.next();
			if (n.key.isDone()) {
				n.value = n.key.get();
				iterator.remove();
			}
		}
	}

	/**
	 * Loads a file into a {@link MemoryUtil#memAlloc(int)} allocated
	 * {@link ByteBuffer}, this buffer needs to be freed using
	 * {@link MemoryUtil#memFree(java.nio.Buffer)}
	 * 
	 * @param resource
	 * @param bufferSize
	 * @return
	 * @throws IOException
	 */
	public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		ByteBuffer buffer;
		
		String resourceFile = FileUtils.fixPath(resource);

		File file = new File(resourceFile);
		if (file.isFile()) {
			try (FileInputStream fis = new FileInputStream(file)) {
				try (FileChannel fc = fis.getChannel()) {
					buffer = memAlloc((int) fc.size() + 1);
					while (fc.read(buffer) != -1)
						;
				}
			}
		} else {
			int size = 0;
			buffer = memAlloc(bufferSize);
			try (InputStream source = ResourcesManager.class.getClassLoader().getResourceAsStream(resource)) {
				if (source == null)
					throw new FileNotFoundException(resource);
				try (ReadableByteChannel rbc = Channels.newChannel(source)) {
					while (true) {
						int bytes = rbc.read(buffer);
						if (bytes == -1)
							break;
						size += bytes;
						if (!buffer.hasRemaining())
							buffer = memRealloc(buffer, size * 2);
					}
				}
			}
			buffer = memRealloc(buffer, size + 1);
		}
		buffer.put((byte) 0);
		buffer.flip();
		return buffer;
	}

}
