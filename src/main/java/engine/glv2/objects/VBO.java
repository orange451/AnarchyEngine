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

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class VBO {

	private final int vboId;
	private final int type;

	private VBO(int vboId, int type) {
		this.vboId = vboId;
		this.type = type;
	}

	public static VBO create(int type) {
		int id = glGenBuffers();
		return new VBO(id, type);
	}

	public void bind() {
		glBindBuffer(type, vboId);
	}

	public void unbind() {
		glBindBuffer(type, 0);
	}

	public void storeData(int[] data, int param) {
		glBufferData(type, data, param);
	}

	public void storeData(IntBuffer data, int param) {
		glBufferData(type, data, param);
	}

	public void storeData(float[] data, int param) {
		glBufferData(type, data, param);
	}

	public void storeData(FloatBuffer data, int param) {
		glBufferData(type, data, param);
	}

	public void dispose() {
		glDeleteBuffers(vboId);
	}

}