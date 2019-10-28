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

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;

import engine.glv2.GLResourcesManagerBackend;
import engine.glv2.objects.Texture;
import engine.tasks.OnFinished;
import engine.tasks.Task;
import engine.tasks.TaskManager;

public final class ResourcesManager {

	public static IResourcesManagerBackend backend = new GLResourcesManagerBackend();
	private static ObjectMap<Task<Texture>, Texture> textureTasks = new ObjectMap<>();

	public static void processShaderIncludes(String file) {
		//TaskManager.submitBackgroundThread(new ShaderIncludeTask(file));
	}

	public static Task<Texture> loadTextureMisc(String fileName, OnFinished<Texture> dst) {
		return loadTextureMisc(fileName, GL_LINEAR, true, dst);
	}

	public static Task<Texture> loadTextureMisc(String fileName, int filter, boolean textureMipMapAF,
			OnFinished<Texture> dst) {
		System.out.println("Texture scheduled to load: " + fileName);
		return TaskManager.submitRenderBackgroundThread(
				new LoadTextureTask(fileName, filter, GL_REPEAT, GL_RGBA, textureMipMapAF)
						.setOnFinished(dst));
	}

	public static Task<Texture> loadTexture(String fileName, OnFinished<Texture> dst) {
		return loadTexture(fileName, GL_LINEAR, true, dst);
	}

	public static Task<Texture> loadTexture(String fileName, int filter, boolean textureMipMapAF,
			OnFinished<Texture> dst) {
		System.out.println("Texture scheduled to load: " + fileName);
		return TaskManager.submitRenderBackgroundThread(
				new LoadTextureTask(fileName, filter, GL_REPEAT, GL_SRGB_ALPHA, textureMipMapAF)
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

	public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		ByteBuffer buffer;

		File file = new File(resource);
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
			try (InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
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
