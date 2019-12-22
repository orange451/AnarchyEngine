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

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_INT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class VAO {

	private static final int BYTES_PER_FLOAT = 4;
	private static final int BYTES_PER_INT = 4;
	public final int id;
	private List<VBO> dataVbos = new ArrayList<>();
	private VBO indexVbo;
	private int indexCount;

	private int vertexCount;

	public static VAO create() {
		int id = glGenVertexArrays();
		return new VAO(id);
	}

	private VAO(int id) {
		this.id = id;
	}

	public int getIndexCount() {
		return indexCount;
	}

	public void bind(int... attributes) {
		bind();
		for (int i : attributes)
			glEnableVertexAttribArray(i);
	}

	public void unbind(int... attributes) {
		for (int i : attributes)
			glDisableVertexAttribArray(i);
		unbind();
	}

	public void createIndexBuffer(int[] indices, int param) {
		this.indexVbo = VBO.create(GL_ELEMENT_ARRAY_BUFFER);
		indexVbo.bind();
		indexVbo.storeData(indices, param);
		this.indexCount = indices.length;
	}

	public void createAttribute(int attribute, int[] data, int attrSize, int param) {
		VBO dataVbo = VBO.create(GL_ARRAY_BUFFER);
		dataVbo.bind();
		dataVbo.storeData(data, param);
		glVertexAttribIPointer(attribute, attrSize, GL_INT, attrSize * BYTES_PER_INT, 0);
		dataVbo.unbind();
		dataVbos.add(dataVbo);
	}

	public void createAttribute(int attribute, IntBuffer data, int attrSize, int param) {
		VBO dataVbo = VBO.create(GL_ARRAY_BUFFER);
		dataVbo.bind();
		dataVbo.storeData(data, param);
		glVertexAttribIPointer(attribute, attrSize, GL_INT, attrSize * BYTES_PER_INT, 0);
		dataVbo.unbind();
		dataVbos.add(dataVbo);
	}

	public void createAttribute(int attribute, float[] data, int attrSize, int param) {
		VBO dataVbo = VBO.create(GL_ARRAY_BUFFER);
		dataVbo.bind();
		dataVbo.storeData(data, param);
		glVertexAttribPointer(attribute, attrSize, GL_FLOAT, false, attrSize * BYTES_PER_FLOAT, 0);
		dataVbo.unbind();
		dataVbos.add(dataVbo);
	}

	public void createAttribute(int attribute, FloatBuffer data, int attrSize, int param) {
		VBO dataVbo = VBO.create(GL_ARRAY_BUFFER);
		dataVbo.bind();
		dataVbo.storeData(data, param);
		glVertexAttribPointer(attribute, attrSize, GL_FLOAT, false, attrSize * BYTES_PER_FLOAT, 0);
		dataVbo.unbind();
		dataVbos.add(dataVbo);
	}

	public void dispose() {
		glDeleteVertexArrays(id);
		for (VBO vbo : dataVbos)
			vbo.dispose();
		if (indexVbo != null)
			indexVbo.dispose();
	}

	public void bind() {
		glBindVertexArray(id);
	}

	public void unbind() {
		glBindVertexArray(0);
	}

	public void setVertexCount(int vertexCount) {
		this.vertexCount = vertexCount;
	}

	public int getVertexCount() {
		return vertexCount;
	}

}