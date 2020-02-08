/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

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

import org.joml.Vector3f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import engine.io.BinaryInputStream;
import engine.io.BinaryOutputStream;
import engine.observer.RenderableMesh;
import engine.tasks.TaskManager;
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
			checkAABB( AABB, vertex ); // Otherwise, just mix AABB with new vertex.
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
	
	private void checkAABB(Pair<Vector3f, Vector3f> tempAABB, Vertex v) {
		if ( v == null )
			return;
		
		float[] xyz = v.getXYZ();
		float x = xyz[0];
		float y = xyz[1];
		float z = xyz[2];

		float mnx = tempAABB.value1().x;
		float mny = tempAABB.value1().y;
		float mnz = tempAABB.value1().z;
		float mxx = tempAABB.value2().x;
		float mxy = tempAABB.value2().y;
		float mxz = tempAABB.value2().z;

		mnx = Math.min(x, mnx);
		mny = Math.min(y, mny);
		mnz = Math.min(z, mnz);
		mxx = Math.max(x, mxx);
		mxy = Math.max(y, mxy);
		mxz = Math.max(z, mxz);

		tempAABB.value1().set(mnx,mny,mnz);
		tempAABB.value2().set(mxx,mxy,mxz);
	}
	
	public void recalculateAABB() {
		Pair<Vector3f, Vector3f> tempAABB = new Pair<Vector3f,Vector3f>(new Vector3f(Integer.MAX_VALUE), new Vector3f(Integer.MIN_VALUE));
		for (int i = 0; i < vertices.length; i++) {
			checkAABB(tempAABB, vertices[i]);
		}
		
		this.AABB = tempAABB;
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
		MemoryUtil.memFree(buffer);

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
		glEnableVertexAttribArray(4);

		// Define the attributes
		boolean normalized = false;
		glVertexAttribPointer(0, Vertex.positionElementCount, GL_FLOAT, normalized, Vertex.stride, Vertex.positionByteOffset);
		glVertexAttribPointer(1, Vertex.normalElementCount, GL_FLOAT,  normalized, Vertex.stride, Vertex.normalByteOffset);
		glVertexAttribPointer(2, Vertex.tangentElementCount, GL_FLOAT,  normalized, Vertex.stride, Vertex.tangetByteOffset);
		glVertexAttribPointer(3, Vertex.textureElementCount, GL_FLOAT,  normalized, Vertex.stride, Vertex.textureByteOffset);
		glVertexAttribPointer(4, Vertex.colorElementCount, GL_FLOAT, normalized, Vertex.stride, Vertex.colorByteOffset);
	}

	/**
	 * Bind this VAO to the OpenGL state.
	 */
	public void bind() {
		if ( modified ) {
			sendToGPU();
		}
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
		TaskManager.addTaskRenderThread(()->{
			GL30.glDeleteVertexArrays(vaoId);
			GL15.glDeleteBuffers(vboId);
		});
		vertices = null;
	}

	@Override
	public void render() {
		glDrawArrays(GL_TRIANGLES, 0, vertices.length);
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
	
	enum MeshFormat {
		NEW_VERTEX(255),
		VERTEX3(0),
		NORMAL3(1),
		UV2(2),
		COLOR4(3),
		VERTEX2(4),
		COLOR3(5),
		TANGENT3(6),
		;
		
		private byte index;
		
		MeshFormat(int index) {
			this.index = (byte) (index & 0xff);
		}
		
		public byte getIndex() {
			return this.index;
		}

		public static MeshFormat match(byte t) {
			MeshFormat[] values = MeshFormat.values();
			
			for (int i = 0; i< values.length; i++) {
				if ( values[i].getIndex() == t )
					return values[i];
			}
			
			return null;
		}
	}
	
	/**
	 * Export a mesh to a given filepath.<br>
	 * The format is simple:<br>
	 * Header: <br> 
	 * 	Char[](4 bytes): MESH<br>
	 * 	Char[](3 bytes): Version number<br>
	 * 	Int(4 bytes): Amount Vertices<br>
	 * <br>
	 * Body(loop): Read byte --> Mesh Command<br>
	 * 	255: New Vertex<br>
	 * 	0: VERTEX3 --> Read 3 floats<br>
	 * 	1: NORMAL3 --> Read 3 floats<br>
	 * 	2: UV2 --> Read 2 floats<br>
	 * 	3: COLOR4 --> Read 4 floats<br>
	 * 	4: VERTEX2 --> Read 2 floats<br>
	 * 	5: COLOR3 --> Read 3 floats<br>
	 * 	6: TANGENT3 --> Read 3 floats<br>
	 * @param mesh
	 * @param filepath
	 */
	public static void Export(BufferedMesh mesh, String filepath) {
		try (BinaryOutputStream bin = new BinaryOutputStream(filepath)) {
			bin.write("MESH100");
			bin.write(mesh.vertices.length);
			for (int i = 0; i < mesh.vertices.length; i++) {
				Vertex v = mesh.vertices[i];
				
				// Mark new Vertex
				bin.write(MeshFormat.NEW_VERTEX.getIndex());
			
				// Write vertex position
				bin.write(MeshFormat.VERTEX3.getIndex());
				bin.write(v.getXYZ());
				
				// Write vertex normal
				bin.write(MeshFormat.NORMAL3.getIndex());
				bin.write(v.getNormalXYZ());
				
				// Write vertex UVs
				bin.write(MeshFormat.UV2.getIndex());
				bin.write(v.getST());
				
				// Write vertex colors
				bin.write(MeshFormat.COLOR4.getIndex());
				bin.write(v.getRGBA());
				
				// Write vertex colors
				bin.write(MeshFormat.TANGENT3.getIndex());
				bin.write(v.getTangentXYZ());
			}
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
			
			// Try to read header
			byte[] b = new byte[4];
			for (int i = 0; i < b.length; i++)
				b[i] = bin.readByte();
			String ISMESH = new String(b);
			
			// Try to read version
			if ( ISMESH.equalsIgnoreCase("MESH") ) {
				byte[] temp = new byte[3];
				for (int i = 0; i < temp.length; i++)
					temp[i] = bin.readByte();
				String VERSION = new String(temp);
				System.out.println("Found version: " + VERSION);
				
				// Read vertices...
				int verts = bin.readInt();
				ret = new BufferedMesh(verts);
				
				Vertex vertex = null;
				boolean addedVertex = false;
				int index = 0;
				
				// Read until empty...
				while(!bin.isEmpty()) {
					MeshFormat readCommand = MeshFormat.match(bin.readByte());
					
					// Try to read new vertex
					if ( readCommand == MeshFormat.NEW_VERTEX ) {
						
						if ( vertex != null && !addedVertex ) {
							addedVertex = true;
							ret.setVertex(index++, vertex);
						}
						
						addedVertex = false;
						vertex = new Vertex();
					}
					
					// Must have vertex to read vertex data
					if ( vertex == null )
						continue;
					
					// Read vector2
					if ( readCommand == MeshFormat.VERTEX2 ) {
						vertex.setXYZ(bin.readFloat(), bin.readFloat(), 0);
						
						if ( !addedVertex ) {
							addedVertex = true;
							ret.setVertex(index++, vertex);
						}
					}
					
					// Read vector3
					if ( readCommand == MeshFormat.VERTEX3 ) {
						vertex.setXYZ(bin.readFloat(), bin.readFloat(), bin.readFloat());
						
						if ( !addedVertex ) {
							addedVertex = true;
							ret.setVertex(index++, vertex);
						}
					}
					
					// Read normal3
					if ( readCommand == MeshFormat.NORMAL3 )
						vertex.setNormalXYZ(bin.readFloat(), bin.readFloat(), bin.readFloat());
					
					// Read uv coordinates
					if ( readCommand == MeshFormat.UV2 )
						vertex.setST(bin.readFloat(), bin.readFloat());
					
					// Read RGB color
					if ( readCommand == MeshFormat.COLOR3 )
						vertex.setRGBA(bin.readFloat(), bin.readFloat(), bin.readFloat(), 1.0f);
					
					// Read RGBA color
					if ( readCommand == MeshFormat.COLOR4 )
						vertex.setRGBA(bin.readFloat(), bin.readFloat(), bin.readFloat(), bin.readFloat());
					
					// Read TANGENT
					if ( readCommand == MeshFormat.TANGENT3 )
						vertex.setTangentXYZ(bin.readFloat(), bin.readFloat(), bin.readFloat());
				}
			} else {
				// Reopen and read LEGACY format... THIS WILL BE REMOVED IN THE FUTURE
				bin.close();
				bin = new BinaryInputStream(filepath);
				
				int verts = bin.readInt();
				ret = new BufferedMesh(verts);
				for (int i = 0; i < verts; i++) {
					Vertex v = new Vertex(
							bin.readFloat(), // X
							bin.readFloat(), // Y
							bin.readFloat(), // Z
							bin.readFloat(), // NX
							bin.readFloat(), // NY
							bin.readFloat(), // NZ
							bin.readFloat(), // S
							bin.readFloat(), // T
							bin.readFloat(), // R
							bin.readFloat(), // G
							bin.readFloat(), // B
							bin.readFloat()  // A
					);
					ret.setVertex(i, v);
				}
				
				// Manually compute tangets!
				// New Mesh format stores them, so use the new one pls!
				ret.computeTangents();
			}
			
			bin.close();
		} catch(Exception e ) {
			System.out.println("Could not load mesh: " + filepath);
		}
		
		return ret;
	}

	public void computeTangents() {
		for (int i = 0; i < vertices.length; i+=3) {
			Vertex v1 = vertices[i+0];
			Vertex v2 = vertices[i+1];
			Vertex v3 = vertices[i+2];
			
			float[] pos1 = v1.getXYZ();
			float[] pos2 = v2.getXYZ();
			float[] pos3 = v3.getXYZ();
			
			float[] uv1 = v1.getST();
			float[] uv2 = v2.getST();
			float[] uv3 = v3.getST();
			
	        float[] deltaPos1 = new float[] {pos2[0]-pos1[0], pos2[1]-pos1[1], pos2[2]-pos1[2]};
	        float[] deltaPos2 = new float[] {pos3[0]-pos1[0], pos3[1]-pos1[1], pos3[2]-pos1[2]};

	        // UV delta
	        float[] deltaUV1 = new float[] {uv2[0]-uv1[0], uv2[1]-uv1[1]};
	        float[] deltaUV2 = new float[] {uv3[0]-uv1[0], uv3[1]-uv1[1]};
	        
	        // Compute tangent
	        float r = 1.0f / (deltaUV1[0] * deltaUV2[1] - deltaUV1[1] * deltaUV2[0]);
	        float[] tangent = new float[] {
	        		((deltaPos1[0] * deltaUV2[1]) - (deltaPos2[0] * deltaUV1[1]))*r,
	        		((deltaPos1[1] * deltaUV2[1]) - (deltaPos2[1] * deltaUV1[1]))*r,
	        		((deltaPos1[2] * deltaUV2[1]) - (deltaPos2[2] * deltaUV1[1]))*r
	        };
	        
	        float[] bitangent = new float[] {
	        		((deltaPos1[0] * -deltaUV2[0]) - (deltaPos2[0] * deltaUV1[0]))*r,
	        		((deltaPos1[1] * -deltaUV2[0]) - (deltaPos2[1] * deltaUV1[0]))*r,
	        		((deltaPos1[2] * -deltaUV2[0]) - (deltaPos2[2] * deltaUV1[0]))*r
	        };
	        
	        // Set tangent vector for triangle
	        v1.setTangentXYZ(tangent[0], tangent[1], tangent[2]);
	        v2.setTangentXYZ(tangent[0], tangent[1], tangent[2]);
	        v3.setTangentXYZ(tangent[0], tangent[1], tangent[2]);
		}
	}
}
