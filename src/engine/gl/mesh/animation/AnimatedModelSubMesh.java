package engine.gl.mesh.animation;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import engine.gl.MaterialGL;
import engine.gl.mesh.Vertex;
import engine.gl.shader.BaseShader;

public class AnimatedModelSubMesh {
	private AnimatedModel parent;
	private Vertex[] vertices;
	private Vector4f[] weightBoneIndices;
	private Vector4f[] weightBoneWeights;
	private FloatBuffer verticesBuffer;
	private FloatBuffer modelBuffer;
	private FloatBuffer normalBuffer;
	protected MaterialGL material;
	private boolean modified;
	private int vaoId = -1;
	private int vboId = -1;
	
	private static final int weightBoneIndicesElementCount = 4;
	private static final int weightBoneWeightElementCount = 4;
	
	private static final int weightBoneIndicesByteCount = weightBoneIndicesElementCount * Vertex.elementBytes;
	private static final int weightBoneWeightByteCount = weightBoneWeightElementCount * Vertex.elementBytes;
	
	private static final int elementCount = Vertex.elementCount + weightBoneIndicesElementCount + weightBoneWeightElementCount;
	private static final int stride       = elementCount * Vertex.elementBytes;
	
	public AnimatedModelSubMesh(AnimatedModel model, int vertices, int bones) {
		this.parent              = model;
		this.vertices            = new Vertex[vertices];
		this.weightBoneIndices   = new Vector4f[vertices];
		this.weightBoneWeights   = new Vector4f[vertices];
		
		this.modelBuffer  = BufferUtils.createFloatBuffer(16);
		this.normalBuffer = BufferUtils.createFloatBuffer(9);
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

	public void render( BaseShader shader, Matrix4f worldMatrix ) {
		if (modified)
			sendToGPU();
		
		// Upload world transformations
		shader.setWorldMatrix(worldMatrix);
		
		// Bind Material
		if (material != null)
			material.bind( shader );
		
		// Draw
		glBindVertexArray(vaoId);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertices.length);
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
		
		int offset1 = Vertex.colorByteOffset + Vertex.colorByteCount;
		int offset2 = offset1 + weightBoneIndicesByteCount;
		
		boolean normalized = false;
		GL20.glVertexAttribPointer(0, Vertex.positionElementCount,   GL11.GL_FLOAT, normalized, stride, Vertex.positionByteOffset);
		GL20.glVertexAttribPointer(1, Vertex.normalElementCount,     GL11.GL_FLOAT, normalized, stride, Vertex.normalByteOffset);
		GL20.glVertexAttribPointer(2, Vertex.textureElementCount,    GL11.GL_FLOAT, normalized, stride, Vertex.textureByteOffset);
		GL20.glVertexAttribPointer(3, Vertex.colorElementCount,      GL11.GL_FLOAT, normalized, stride, Vertex.colorByteOffset);
		GL20.glVertexAttribPointer(4, weightBoneIndicesElementCount, GL11.GL_FLOAT, normalized, stride, offset1);
		GL20.glVertexAttribPointer(5, weightBoneWeightElementCount,  GL11.GL_FLOAT, normalized, stride, offset2);
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
}
