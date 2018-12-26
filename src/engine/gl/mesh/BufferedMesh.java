package engine.gl.mesh;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import engine.gl.MaterialGL;
import engine.gl.shader.BaseShader;
import engine.io.BinaryInputStream;
import engine.io.BinaryOutputStream;
import engine.observer.RenderableMesh;
import engine.util.Pair;

public class BufferedMesh implements RenderableMesh {
	private int vaoId;
	private int vboId;
	private Vertex[] vertices;
	private boolean modified;
	private Pair<Vector3f,Vector3f> AABB;

	public BufferedMesh(int vertices) {
		resize(vertices);
		recalculateAABB();
	}

	/**
	 * Set a vertex at a given index for this mesh.
	 * @param index
	 * @param vertex
	 */
	public void setVertex(int index, Vertex vertex) {
		boolean r = vertices[index] != null;
		
		// Add vertex
		modified = true;
		vertices[index] = vertex;
		
		// If a vertex was overridden, we cant be sure how it'll affect the AABB.
		// So we will recalculate the whole thing.
		if ( r ) {
			recalculateAABB();
		} else {
			checkAABB( vertex ); // Otherwise, just mix AABB with new vertex.
		}
	}

	/**
	 * Get the vertex at a given index.
	 * @param i
	 * @return
	 */
	public Vertex getVertex(int i) {
		return vertices[i];
	}

	/**
	 * Return the array of vertices for this mesh.
	 * @return
	 */
	public Vertex[] getVertices() {
		return this.vertices;
	}

	/**
	 * Change the amount of vertices used in this mesh.
	 * @param size
	 */
	public void resize(int size) {
		// Store old vertices.
		Vertex[] old = vertices;

		// Make new vertex list.
		vertices = new Vertex[size];
		modified = true;

		// Try to fill old vertices into new list.
		if ( old != null ) {
			for (int i = 0; i < Math.min(size, old.length); i++) {
				vertices[i] = old[i];
			}
		}
	}
	
	public void scale(float scale) {
		// Loop through each vertex, scale it
		for (int i = 0; i < vertices.length; i++) {
			Vertex v = vertices[i];
			if ( v == null )
				continue;
			
			float[] xyz = v.getXYZ();
			v.setXYZ(xyz[0]*scale, xyz[1]*scale, xyz[2]*scale);
		}
		
		// Mark vertices as modified (reubmit to GPU next draw call)
		modified = true;
	}

	/**
	 * Return the amount of vertices in the mesh
	 * @return
	 */
	public int getSize() {
		return vertices.length;
	}

	/**
	 * If there are any null vertices at the end of the vertex array, they get removed,
	 * and the {@link #resize(int)} method is called to shorten the array.
	 */
	public void clip() {
		// Get first vertex index that is null.
		int firstNullIndex = getFirstNullVertex();
		if ( firstNullIndex == -1 )
			return;

		// Resize the array to n-len.
		resize(firstNullIndex);
	}

	/**
	 * Flips the drawing direction of all faces. Also inverts normals.
	 */
	public void flipFaces() {
		this.invertNormals();

		//Create a temp copy of the vertices
		Vertex[] v = new Vertex[vertices.length];
		for (int i = 0; i < v.length; i++) {
			v[i] = vertices[i];
		}

		// Invert the vertices
		for (int i = 0; i < v.length; i++)
			vertices[(v.length - 1) - i] = v[i];

		modified = true;
	}

	/**
	 * Flip the direction of all normals in this mesh.
	 */
	public void invertNormals() {
		for (int i = 0; i < vertices.length; i++) {
			Vertex vertex = vertices[i];
			float[] normal = vertex.getNormalXYZ();
			normal[0] *= -1;
			normal[1] *= -1;
			normal[2] *= -1;
			vertex.setNormalXYZ( normal[0], normal[1], normal[2] );
		}
		modified = true;
	}
	
	/**
	 * Return the current mesh's AABB.
	 * @return
	 */
	public Pair<Vector3f, Vector3f> getAABB() {
		return this.AABB;
	}
	
	private void checkAABB(Vertex v) {
		if ( v == null )
			return;
		
		float[] xyz = v.getXYZ();
		float x = xyz[0];
		float y = xyz[1];
		float z = xyz[2];

		float mnx = AABB.value1().x;
		float mny = AABB.value1().y;
		float mnz = AABB.value1().z;
		float mxx = AABB.value2().x;
		float mxy = AABB.value2().y;
		float mxz = AABB.value2().z;

		mnx = Math.min(x, mnx);
		mny = Math.min(y, mny);
		mnz = Math.min(z, mnz);
		mxx = Math.max(x, mxx);
		mxy = Math.max(y, mxy);
		mxz = Math.max(z, mxz);

		AABB.value1().set(mnx,mny,mnz);
		AABB.value2().set(mxx,mxy,mxz);
	}
	
	private void recalculateAABB() {
		AABB = new Pair<Vector3f,Vector3f>(new Vector3f(Integer.MAX_VALUE), new Vector3f(Integer.MIN_VALUE));
		for (int i = 0; i < vertices.length; i++) {
			checkAABB(vertices[i]);
		}
	}


	/**
	 * Returns the first vertex index that is null.
	 * @return
	 */
	protected int getFirstNullVertex() {
		// Get first vertex index that is null.
		int firstNullIndex = -1;
		for (int i = 0; i < vertices.length; i++) {
			if ( vertices[i] == null ) {
				firstNullIndex = i;
				break;
			}
		}

		return firstNullIndex;
	}

	protected void sendToGPU() {
		// Initial vertex data
		FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length * Vertex.elementCount);
		for (int i = 0; i < vertices.length; i++) {
			buffer.put(vertices[i].getElements());
		}
		buffer.flip();

		// Generate buffers
		vboId = glGenBuffers();
		vaoId = glGenVertexArrays();

		// Upload Vertex Buffer
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

		// Set attributes (automatically stored to currently bound VAO)
		bindVertexAttributes();

		// Unbind
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);

		// Clear buffer
		buffer.clear();
		buffer = null;

		// Reset modified flag
		modified = false;
	}

	protected void bindVertexAttributes() {
		// Enable the attributes
		glBindVertexArray(vaoId);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glEnableVertexAttribArray(3);

		// Define the attributes
		boolean normalized = false;
		glVertexAttribPointer(0, Vertex.positionElementCount, GL_FLOAT, normalized, Vertex.stride, Vertex.positionByteOffset);
		glVertexAttribPointer(1, Vertex.normalElementCount, GL_FLOAT,  normalized, Vertex.stride, Vertex.normalByteOffset);
		glVertexAttribPointer(2, Vertex.textureElementCount, GL_FLOAT,  normalized, Vertex.stride, Vertex.textureByteOffset);
		glVertexAttribPointer(3, Vertex.colorElementCount, GL_FLOAT, normalized, Vertex.stride, Vertex.colorByteOffset);
	}

	/**
	 * Bind this VAO to the OpenGL state.
	 */
	public void bind() {
		glBindVertexArray(vaoId);
	}

	/**
	 * Unbind the VAO from the OpenGL state.
	 */
	public void unbind() {
		GL30.glBindVertexArray(0);
	}

	/**
	 * Cleans up all resources used by the BufferedMesh. After cleaning, the mesh can no longer be drawn.
	 * It must be re-filled with vertex data.
	 */
	public void cleanup() {
		GL30.glDeleteVertexArrays(vaoId);
		GL15.glDeleteBuffers(vboId);
		vertices = null;
	}

	/**
	 * Render this mesh at a given world matrix with a given material.
	 * @param genericShader
	 * @param worldMatrix
	 * @param material
	 */
	public void render(BaseShader shader, Matrix4f worldMatrix, MaterialGL material) {
		if ( modified ) {
			sendToGPU();
		}

		// Bind the material for drawing
		if ( material != null ) {
			material.bind(shader);
		}

		// Bind the VAO for drawing
		this.bind();

		// Set world matrix
		if ( worldMatrix != null ) {
			shader.setWorldMatrix(worldMatrix);
		}

		// Draw
		glDrawArrays(GL_TRIANGLES, 0, vertices.length);

		// Unbind VAO
		this.unbind();
	}

	/**
	 * Combine a list of BufferedMeshes together into one mesh.
	 * @param meshes
	 * @return
	 */
	public static BufferedMesh combineMeshes( BufferedMesh... meshes ) {
		int size = 0;
		for (int i = 0; i < meshes.length; i++) {
			if ( meshes[i] == null )
				continue;

			size += meshes[i].getSize();
		}

		int pointer = 0;
		BufferedMesh ret = new BufferedMesh( size );
		for (int i = 0; i < meshes.length; i++) {
			BufferedMesh mesh = meshes[i];
			if ( mesh == null )
				continue;

			Vertex[] verts = mesh.getVertices();
			for (int j = 0; j < verts.length; j++) {
				ret.setVertex( pointer++, new Vertex(verts[j]) );
			}
		}
		return ret;
	}
	
	/**
	 * Export a mesh to a given filepath.<br>
	 * The format is simple:<br>
	 * Header: 4-bytes (int) --> Amount vertices
	 * <br>
	 * Body: 48-bytes per vertex (12 ints per vertex) --> Vertex data
	 * <br>
	 * 	Int 1: --> Vertex x<br>
	 * 	Int 2: --> Vertex y<br>
	 * 	Int 3: --> Vertex z<br>
	 * 	Int 4: --> Vertex normal x<br>
	 * 	Int 5: --> Vertex normal y<br>
	 * 	Int 6: --> Vertex normal z<br>
	 * 	Int 7: --> Vertex UV s<br>
	 * 	Int 8: --> Vertex UV t<br>
	 * 	Int 9:  --> Vertex Color r<br>
	 * 	Int 10: --> Vertex Color g<br>
	 * 	Int 11: --> Vertex Color b<br>
	 * 	Int 12: --> Vertex Color a<br>
	 * @param mesh
	 * @param filepath
	 */
	public static void Export(BufferedMesh mesh, String filepath) {
		try {
			BinaryOutputStream bin = new BinaryOutputStream(filepath);
			bin.write(mesh.vertices.length);
			for (int i = 0; i < mesh.vertices.length; i++) {
				Vertex v = mesh.vertices[i];
				bin.write(v.getXYZ());
				bin.write(v.getNormalXYZ());
				bin.write(v.getST());
				bin.write(v.getRGBA());
			}
			
			bin.close();
		} catch (Exception e) {
			//
		}
	}

	/**
	 * Import a mesh from a given filepath.
	 * @param filepath
	 * @return
	 */
	public static BufferedMesh Import(String filepath) {
		//System.out.println("Reading mesh: " + filepath);
		BufferedMesh ret = null;
		try {
			BinaryInputStream bin = new BinaryInputStream(filepath);
			int verts = bin.readInt();
			
			ret = new BufferedMesh(verts);
			for (int i = 0; i < verts; i++) {
				Vertex v = new Vertex(
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat(),
						bin.readFloat()
				);
				ret.setVertex(i, v);
			}
			
		} catch(Exception e ) {
			e.printStackTrace();
		}
		
		return ret;
	}
}
