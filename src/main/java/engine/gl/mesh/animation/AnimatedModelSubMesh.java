/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.mesh.animation;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;

import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.Vertex;

public class AnimatedModelSubMesh {
	private Vertex[] vertices;
	private Vector4f[] weightBoneIndices;
	private Vector4f[] weightBoneWeights;
	private FloatBuffer verticesBuffer;
	private boolean modified;
	private int vaoId = -1;
	private int vboId = -1;
	
	private static final int weightBoneIndicesElementCount = 4;
	private static final int weightBoneWeightElementCount = 4;
	
	private static final int weightBoneIndicesByteCount = weightBoneIndicesElementCount * Vertex.elementBytes;
	@SuppressWarnings("unused")
	private static final int weightBoneWeightByteCount = weightBoneWeightElementCount * Vertex.elementBytes;
	
	private static final int elementCount = Vertex.elementCount + weightBoneIndicesElementCount + weightBoneWeightElementCount;
	private static final int stride       = elementCount * Vertex.elementBytes;
	
	public AnimatedModelSubMesh(BufferedMesh source) {
		int verts = source.getSize();
		this.vertices            = new Vertex[verts];
		this.weightBoneIndices   = new Vector4f[verts];
		this.weightBoneWeights   = new Vector4f[verts];
		
		for (int i = 0; i < verts; i++) {
			this.setVertex(source.getVertex(i), i);

			this.setBoneIndices(i, new Vector4f(0));
			this.setBoneWeights(i, new Vector4f(0));
		}
	}
	
	public void setVertex(Vertex v, int index) {
		this.vertices[index] = v;
		modified = true;
	}
	
	public void setBoneIndices(int index, Vector4f bonedex) {
		this.weightBoneIndices[index] = bonedex;
	}
	
	public void setBoneWeights(int index, Vector4f weights) {
		this.weightBoneWeights[index] = weights;
	}

	public void bind() {
		if (modified)
			sendToGPU();
		
		glBindVertexArray(vaoId);
	}

	public void unbind() {
		glBindVertexArray(0);
	}

	private void sendToGPU() {
		// Create new vertex buffer
		if (this.verticesBuffer == null) {
			this.verticesBuffer = BufferUtils.createFloatBuffer(vertices.length * elementCount);
		} else {
			this.verticesBuffer.clear();
		}
		
		// Fill vertex buffer
		for (int i = 0; i < vertices.length; i++) {
			this.verticesBuffer.put(this.getElements(i));
		}
		this.verticesBuffer.flip();
		
		// Generate a new VBO id
		if (vboId == -1)
			vboId = glGenBuffers();
		
		if (vaoId == -1)
			vaoId = glGenVertexArrays();
		
		// Upload vertex buffer
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
		glBindVertexArray(vaoId);
		
		// Setup attributes
		bindVertexAttributes();
		
		// Unbind Model
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
		
		modified = false;
	}

	private static void bindVertexAttributes() {
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glEnableVertexAttribArray(3);
		glEnableVertexAttribArray(4);
		glEnableVertexAttribArray(5);
		glEnableVertexAttribArray(6);
		
		int offset1 = Vertex.colorByteOffset + Vertex.colorByteCount;
		int offset2 = offset1 + weightBoneIndicesByteCount;
		
		boolean normalized = false;
		GL20.glVertexAttribPointer(0, Vertex.positionElementCount,   GL11.GL_FLOAT, normalized, stride, Vertex.positionByteOffset);
		GL20.glVertexAttribPointer(1, Vertex.normalElementCount,     GL11.GL_FLOAT, normalized, stride, Vertex.normalByteOffset);
		GL20.glVertexAttribPointer(2, Vertex.tangentElementCount,    GL11.GL_FLOAT, normalized, stride, Vertex.tangetByteOffset);
		GL20.glVertexAttribPointer(3, Vertex.textureElementCount,    GL11.GL_FLOAT, normalized, stride, Vertex.textureByteOffset);
		GL20.glVertexAttribPointer(4, Vertex.colorElementCount,      GL11.GL_FLOAT, normalized, stride, Vertex.colorByteOffset);
		GL20.glVertexAttribPointer(5, weightBoneIndicesElementCount, GL11.GL_FLOAT, normalized, stride, offset1);
		GL20.glVertexAttribPointer(6, weightBoneWeightElementCount,  GL11.GL_FLOAT, normalized, stride, offset2);
	}
	
	private float[] getElements(int vertex) {
		float[] vElement = vertices[vertex].getElements();
		float[] ret = new float[elementCount];
		int i = 0;
		
		// Fill with vertex data
		for (i = 0; i < vElement.length; i++) {
			ret[i] = vElement[i];
		}
		
		// Fill with weight data
		ret[i++] = weightBoneIndices[vertex].x;
		ret[i++] = weightBoneIndices[vertex].y;
		ret[i++] = weightBoneIndices[vertex].z;
		ret[i++] = weightBoneIndices[vertex].w;
		ret[i++] = weightBoneWeights[vertex].x;
		ret[i++] = weightBoneWeights[vertex].y;
		ret[i++] = weightBoneWeights[vertex].z;
		ret[i++] = weightBoneWeights[vertex].w;
		return ret;
	}

	public Vertex[] getVertices() {
		return this.vertices;
	}

	public int size() {
		return this.vertices.length;
	}
}
