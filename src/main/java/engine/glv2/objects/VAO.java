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