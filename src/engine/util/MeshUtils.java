package engine.util;

import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMaterialProperty;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.BufferedPrefab;
import engine.gl.mesh.Quad;
import engine.gl.mesh.QuadStrip;
import engine.gl.mesh.Triangle;
import engine.gl.mesh.TriangleFan;
import engine.gl.mesh.TriangleStrip;
import engine.gl.mesh.Vertex;
import engine.gl.mesh.mm3d.Mm3dImporter;
import luaengine.type.data.Color3;
import luaengine.type.object.insts.Material;
import luaengine.type.object.insts.Mesh;
import luaengine.type.object.insts.Prefab;
import luaengine.type.object.insts.Texture;

public class MeshUtils {
	private static final int CACHE_SIZE = 240;
	private static Vector3f OFFSET = new Vector3f();

	static {
		// Create a mesh to initialize the teapot
		TeapotData.mesh(1.0f);
	}

	private static BufferedMesh disk( float innerRadius, float outerRadius, int slices, int loops ) {
		return partialDisk( innerRadius, outerRadius, slices, loops, 0, 360 );
	}

	private static BufferedMesh partialDisk( float innerRadius, float outerRadius, int slices, int loops, float startAngle, float sweepAngle ) {

		float[] sinCache = new float[CACHE_SIZE];
		float[] cosCache = new float[CACHE_SIZE];
		float angle;
		float deltaRadius;
		float radiusLow;
		float texLow = 0.0f;
		float angleOffset;
		// Limit steps
		if ( slices >= CACHE_SIZE )
			slices = CACHE_SIZE-1;

		// Verify parameters
		if ( slices < 2 || loops < 1 || outerRadius <= 0.0 || innerRadius < 0.0 ) {
			System.err.println("Invalid parameters");
			return null;
		}

		// Fix angles
		if (sweepAngle < -360.0) sweepAngle = 360.0f;
		if (sweepAngle > 360.0) sweepAngle = 360.0f;
		if (sweepAngle < 0) {
			startAngle += sweepAngle;
			sweepAngle = -sweepAngle;
		}
		if (sweepAngle == 360.0) {
		} else {
		}

		/* Compute length (needed for normal calculations) */
		deltaRadius = outerRadius - innerRadius;

		/* Cache is the vertex locations cache */
		angleOffset = (float) (startAngle / 180.0 * Math.PI);
		for (int i = 0; i <= slices; i++) {
			angle = (float) (angleOffset + ((Math.PI * sweepAngle) / 180.0) * i / slices);
			sinCache[i] = (float) Math.sin(angle);
			cosCache[i] = (float) Math.cos(angle);
		}

		if (sweepAngle == 360.0) {
			sinCache[slices] = sinCache[0];
			cosCache[slices] = cosCache[0];
		}

		// Compute mesh data
		TriangleFan triangleFan = new TriangleFan();
		if (innerRadius == 0.0) {
			// Center vertex
			Vertex center = new Vertex( 0, 0, 0, 0, 0, 1, 0.5f, 0.5f );
			center.setXYZ( center.getXYZ()[0] + OFFSET.x, center.getXYZ()[1] + OFFSET.y, center.getXYZ()[2] + OFFSET.z );
			triangleFan.addVertex( center );

			radiusLow = outerRadius - deltaRadius * ((float) (loops-1) / loops);
			texLow = radiusLow / outerRadius / 2;

			for (int i = slices; i >= 0; i--) {
				Vertex vertex = new Vertex(radiusLow * sinCache[i], radiusLow * cosCache[i], 0.0f, 0, 0, 1, texLow * sinCache[i] + 0.5f, texLow * cosCache[i] + 0.5f);
				vertex.setXYZ( vertex.getXYZ()[0] + OFFSET.x, vertex.getXYZ()[1] + OFFSET.y, vertex.getXYZ()[2] + OFFSET.z );
				triangleFan.addVertex( vertex );
			}
		}

		// Create final mesh
		int pointer = 0;
		ArrayList<Triangle> tris = triangleFan.getTriangles();
		BufferedMesh ret = new BufferedMesh( tris.size() * 3 );
		for (int i = 0; i < tris.size(); i++) {
			Triangle triangle = tris.get(i);
			ret.setVertex(pointer++, triangle.getVertex(0));
			ret.setVertex(pointer++, triangle.getVertex(1));
			ret.setVertex(pointer++, triangle.getVertex(2));
		}
		return ret;
	}

	private static BufferedMesh torus( float outerRadius, float innerRadius, int stepsH, int stepsV ) {

		float outerStep = 2.0f * (float)Math.PI / (float)stepsH;
		float innerStep = 2.0f * (float)Math.PI / (float)stepsV;

		TriangleStrip triangleStrip = new TriangleStrip();
		for (int i = 0; i < stepsH; ++i) {
			float a0 = i * outerStep;
			float a1 = a0 + outerStep;
			float x0 = (float) Math.cos(a0);
			float y0 = (float) Math.sin(a0);
			float x1 = (float) Math.cos(a1);
			float y1 = (float) Math.sin(a1);

			triangleStrip.snip();
			for (int j = 0; j <= stepsV; ++j ) {
				float b = j * innerStep;
				float c = (float) Math.cos(b);
				float r = innerRadius * c + outerRadius;
				float z = innerRadius * (float) Math.sin(b);

				// Calculate vertex data
				Vector2f texture1  = new Vector2f( (float)(i)/(float)(stepsH), (float)(j)/(float)(stepsV) );
				Vector3f normal1   = new Vector3f( x0 * c, y0 * c, z/innerRadius );
				normal1.normalize();
				Vector3f position1 = new Vector3f( x0 * r, y0 * r, z );

				// Calculate next vertex data
				Vector2f texture2  = new Vector2f( (float)(i+1)/(float)(stepsH), (float)(j)/(float)(stepsV) );
				Vector3f normal2   = new Vector3f( x1 * c, y1 * c, z/innerRadius );
				normal1.normalize();
				Vector3f position2 = new Vector3f( x1 * r, y1 * r, z );

				// Add vertices to triangle strip
				triangleStrip.addVertex( new Vertex( position1.x, position1.y, position1.z, normal1.x, normal1.y, normal1.z, texture1.x, texture1.y ) );
				triangleStrip.addVertex( new Vertex( position2.x, position2.y, position2.z, normal2.x, normal2.y, normal2.z, texture2.x, texture2.y ) );
			}
		}

		int pointer = 0;
		ArrayList<Triangle> tris = triangleStrip.getTriangles();
		BufferedMesh ret = new BufferedMesh( tris.size() * 3 );
		for (int i = 0; i < tris.size(); i++) {
			Triangle triangle = tris.get(i);
			if ( i % 2 == 0 ) {
				ret.setVertex(pointer++, triangle.getVertex(0));
				ret.setVertex(pointer++, triangle.getVertex(1));
				ret.setVertex(pointer++, triangle.getVertex(2));
			} else {
				ret.setVertex(pointer++, triangle.getVertex(2));
				ret.setVertex(pointer++, triangle.getVertex(1));
				ret.setVertex(pointer++, triangle.getVertex(0));
			}
		}
		return ret;
	}

	private static BufferedMesh cylinder( float baseRadius, float topRadius, float height, int slices, int stacks ) {
		float[] sinCache = new float[CACHE_SIZE];
		float[] cosCache = new float[CACHE_SIZE];
		float angle;
		float zLow, zHigh;
		float length;
		float deltaRadius;
		float zNormal;
		float radiusLow, radiusHigh;

		// Limit steps
		if ( slices >= CACHE_SIZE )
			slices = CACHE_SIZE-1;

		// Verify parameters
		if ( slices < 2 || stacks < 1 || baseRadius < 0.0 || topRadius < 0.0 || height < 0.0 ) {
			System.err.println("Invalid parameters");
			return null;
		}

		// Compute length (needed for normal calculations)
		deltaRadius = baseRadius - topRadius;
		length = (float) Math.sqrt(deltaRadius*deltaRadius + height*height);
		if ( length == 0.0 ) {
			System.err.println("Invalid parameters");
			return null;
		}

		zNormal = deltaRadius / length;
		for (int i = 0; i < slices; i++) {
			angle = (float) (2 * Math.PI * i / (float)slices);
			sinCache[i] = (float) Math.sin(angle);
			cosCache[i] = (float) Math.cos(angle);
		}

		sinCache[slices] = sinCache[0];
		cosCache[slices] = cosCache[0];

		// Compute Mesh quads
		QuadStrip quadStrip = new QuadStrip();

		for (int j = 0; j < stacks; j++) {
			quadStrip.snip();
			zLow = j * height / (float)stacks - (height/2f);
			zHigh = (j + 1) * height / (float)stacks - (height/2f);
			radiusLow = baseRadius - deltaRadius * ((float) j / (float)stacks);
			radiusHigh = baseRadius - deltaRadius * ((float) (j + 1) / (float)stacks);

			for (int i = 0; i <= slices; i++) {

				// Find normal
				Vector3f normal = new Vector3f();
				normal.x = sinCache[i];
				normal.y = cosCache[i];
				normal.z = zNormal;

				// Find other vertex data
				Vector2f texture1  = new Vector2f( 1 - (float)i/(float)slices, (float)j/(float)stacks);
				Vector3f position1 = new Vector3f( radiusLow * sinCache[i], radiusLow * cosCache[i], zLow );

				Vector2f texture2  = new Vector2f( 1 - (float)i/(float)slices, (float)(j+1)/(float)stacks);
				Vector3f position2 = new Vector3f( radiusHigh * sinCache[i], radiusHigh * cosCache[i], zHigh );

				position1.add(OFFSET);
				position2.add(OFFSET);

				// Add verts to the strip
				quadStrip.addVertex( new Vertex( position1.x, position1.y, position1.z, normal.x, normal.y, normal.z, texture1.x, texture1.y ) );
				quadStrip.addVertex( new Vertex( position2.x, position2.y, position2.z, normal.x, normal.y, normal.z, texture2.x, texture2.y ) );
			}
		}

		// Compute final Mesh
		int pointer = 0;
		ArrayList<Quad> quads = quadStrip.getQuads();
		BufferedMesh mesh = new BufferedMesh( (int)(quads.size() * 6) );
		for (int i = 0; i < quads.size(); i++) {
			Triangle[] tris = quads.get(i).triangulate();

			if ( tris == null )
				continue;

			mesh.setVertex( pointer++, tris[0].getVertex(0) );
			mesh.setVertex( pointer++, tris[0].getVertex(1) );
			mesh.setVertex( pointer++, tris[0].getVertex(2) );

			mesh.setVertex( pointer++, tris[1].getVertex(0) );
			mesh.setVertex( pointer++, tris[1].getVertex(1) );
			mesh.setVertex( pointer++, tris[1].getVertex(2) );
		}

		// Create tops
		OFFSET.z -= height/2f;
		BufferedMesh bottom = disk( 0, baseRadius, slices, 1 );
		bottom.flipFaces();
		OFFSET.z += height;
		BufferedMesh top = disk( 0, topRadius, slices, 1 );
		OFFSET.z -= height/2f;

		// Return final mesh
		return BufferedMesh.combineMeshes( mesh, bottom, top );
	}

	private static BufferedMesh sphere( float radius, int slices, int stacks ) {
		float[] sinCache1a = new float[CACHE_SIZE];
		float[] cosCache1a = new float[CACHE_SIZE];
		float[] sinCache1b = new float[CACHE_SIZE];
		float[] cosCache1b = new float[CACHE_SIZE];
		float[] sinCache2a = new float[CACHE_SIZE];
		float[] cosCache2a = new float[CACHE_SIZE];
		float[] sinCache2b = new float[CACHE_SIZE];
		float[] cosCache2b = new float[CACHE_SIZE];
		float angle;
		float zLow, zHigh;
		float sintemp1 = 0.0f, sintemp2 = 0.0f, sintemp3 = 0.0f, sintemp4 = 0.0f;
		float costemp3 = 0.0f, costemp4 = 0.0f;
		int start, finish;

		if (slices >= CACHE_SIZE) slices = CACHE_SIZE-1;
		if (stacks >= CACHE_SIZE) stacks = CACHE_SIZE-1;
		if (slices < 2 || stacks < 1 || radius < 0.0) {
			System.err.println("Invalid parameters");
			return null;
		}

		for (int i = 0; i < slices; i++) {
			angle = (float) (2 * Math.PI * i / (float)slices);
			sinCache1a[i] = (float) Math.sin(angle);
			cosCache1a[i] = (float) Math.cos(angle);
			sinCache2a[i] = sinCache1a[i];
			cosCache2a[i] = cosCache1a[i];
		}

		for (int j = 0; j <= stacks; j++) {
			angle = (float) (Math.PI * j / (float)stacks);
			sinCache2b[j] = (float) Math.sin(angle);
			cosCache2b[j] = (float) Math.cos(angle);
			sinCache1b[j] = (float) (radius * Math.sin(angle));
			cosCache1b[j] = (float) (radius * Math.cos(angle));
		}

		/* Make sure it comes to a point */
		sinCache1b[0] = 0;
		sinCache1b[stacks] = 0;

		sinCache1a[slices] = sinCache1a[0];
		cosCache1a[slices] = cosCache1a[0];
		sinCache2a[slices] = sinCache2a[0];
		cosCache2a[slices] = cosCache2a[0];

		// Compute Mesh quads
		QuadStrip quadStrip = new QuadStrip();
		start = 0;
		finish = stacks;
		for (int j = start; j < finish; j++) {
			zLow = cosCache1b[j];
			zHigh = cosCache1b[j+1];
			sintemp1 = sinCache1b[j];
			sintemp2 = sinCache1b[j+1];

			sintemp3 = sinCache2b[j+1];
			costemp3 = cosCache2b[j+1];
			sintemp4 = sinCache2b[j];
			costemp4 = cosCache2b[j];

			quadStrip.snip();
			for (int i = 0; i <= slices; i++) {
				Vector3f normal1 = new Vector3f(sinCache2a[i] * sintemp3, cosCache2a[i] * sintemp3, costemp3 );
				Vector2f texture1 = new Vector2f(1 - (float) i / slices, 1 - (float) (j+1) / stacks);
				Vector3f position1 = new Vector3f(sintemp2 * sinCache1a[i], sintemp2 * cosCache1a[i], zHigh);

				Vector3f normal2 = new Vector3f(sinCache2a[i] * sintemp4,cosCache2a[i] * sintemp4,costemp4);
				Vector2f texture2 = new Vector2f(1 - (float) i / slices,1 - (float) j / stacks);
				Vector3f position2 = new Vector3f(sintemp1 * sinCache1a[i],sintemp1 * cosCache1a[i], zLow);

				position1.add(OFFSET);
				position2.add(OFFSET);

				quadStrip.addVertex( new Vertex( position1.x, position1.y, position1.z, normal1.x, normal1.y, normal1.z, texture1.x, texture1.y ) );
				quadStrip.addVertex( new Vertex( position2.x, position2.y, position2.z, normal2.x, normal2.y, normal2.z, texture2.x, texture2.y ) );
			}
		}

		// Compute final Mesh
		int pointer = 0;
		ArrayList<Quad> quads = quadStrip.getQuads();
		BufferedMesh mesh = new BufferedMesh( (int)(quads.size() * 6) );
		for (int i = 0; i < quads.size(); i++) {
			Triangle[] tris = quads.get(i).triangulate();

			if ( tris == null )
				continue;

			mesh.setVertex( pointer++, tris[0].getVertex(0) );
			mesh.setVertex( pointer++, tris[0].getVertex(1) );
			mesh.setVertex( pointer++, tris[0].getVertex(2) );

			mesh.setVertex( pointer++, tris[1].getVertex(0) );
			mesh.setVertex( pointer++, tris[1].getVertex(1) );
			mesh.setVertex( pointer++, tris[1].getVertex(2) );
		}
		return mesh;
	}

	public static BufferedMesh quad(float x1, float y1, float x2, float y2, float tx1, float ty1, float tx2, float ty2) {
		BufferedMesh mesh = new BufferedMesh( 6 );
		mesh.setVertex(0, new Vertex( x1, y1, 0, 0, 0, 1,  tx1, ty1 ));
		mesh.setVertex(1, new Vertex( x2, y1, 0, 0, 0, 1,  tx2, ty1 ));
		mesh.setVertex(2, new Vertex( x2, y2, 0, 0, 0, 1,  tx2, ty2 ));
		mesh.setVertex(3, new Vertex( x2, y2, 0, 0, 0, 1,  tx2, ty2 ));
		mesh.setVertex(4, new Vertex( x1, y2, 0, 0, 0, 1,  tx1, ty2 ));
		mesh.setVertex(5, new Vertex( x1, y1, 0, 0, 0, 1,  tx1, ty1 ));

		return mesh;
	}

	public static BufferedMesh quad( float width, float height ) {
		return quad( 0, 0, width, height, 0, 0, 1, 1 );
	}

	public static BufferedMesh torus( float outerRadius, float innerRadius, int steps ) {
		return torus( outerRadius, innerRadius, steps, steps );
	}

	public static BufferedMesh capsule( float radius, float height, int steps ) {
		height -= radius*2;
		if ( height < 0 )
			return null;

		float offsetOriginal = OFFSET.z;
		BufferedMesh mesh2 = cylinder( radius, height, steps );
		OFFSET.z += height/2;
		BufferedMesh mesh1 = sphere( radius, steps, steps );
		OFFSET.z -= height;
		BufferedMesh mesh3 = sphere( radius, steps, steps );
		OFFSET.z = offsetOriginal;

		BufferedMesh ret = BufferedMesh.combineMeshes( mesh1, mesh2, mesh3 );
		return ret;
	}

	public static BufferedMesh plane(float x1, float y1, float z1, float x2, float y2, float z2, float hrep, float vrep) {
		BufferedMesh mesh = new BufferedMesh( 6 );
		mesh.setVertex(0, new Vertex( x1, y1, z1, 0, 0, 1,  0,    0 ));
		mesh.setVertex(1, new Vertex( x2, y1, z1, 0, 0, 1,  hrep, 0 ));
		mesh.setVertex(2, new Vertex( x2, y2, z2, 0, 0, 1,  hrep, vrep ));
		mesh.setVertex(3, new Vertex( x2, y2, z2, 0, 0, 1,  hrep, vrep ));
		mesh.setVertex(4, new Vertex( x1, y2, z2, 0, 0, 1,  0,    vrep ));
		mesh.setVertex(5, new Vertex( x1, y1, z1, 0, 0, 1,  0,    0 ));

		return mesh;
	}


	public static BufferedMesh block( float width, float length, float height ) {
		return box( -width/2, -length/2, height/2, width/2, length/2, -height/2, 1, 1 );
	}

	public static BufferedMesh box( float x1, float y1, float z1, float x2, float y2, float z2, float repx, float repy ) {
		Vector3f v1 = new Vector3f( x1, y1, z1 );
		Vector3f v2 = new Vector3f( x2, y1, z1 );
		Vector3f v3 = new Vector3f( x2, y2, z1 );
		Vector3f v4 = new Vector3f( x1, y2, z1 );
		Vector3f v5 = new Vector3f( x1, y1, z2 );
		Vector3f v6 = new Vector3f( x2, y1, z2 );
		Vector3f v7 = new Vector3f( x2, y2, z2 );
		Vector3f v8 = new Vector3f( x1, y2, z2 );

		BufferedMesh model = new BufferedMesh( 36 );
		model.setVertex(0,  new Vertex( v1.x, v1.y, v1.z, 0, 0, 1, 0, repy ) );
		model.setVertex(1,  new Vertex( v2.x, v2.y, v2.z, 0, 0, 1, repx, repy ) );
		model.setVertex(2,  new Vertex( v3.x, v3.y, v3.z, 0, 0, 1, repx, 0 ) );
		model.setVertex(3,  new Vertex( v3.x, v3.y, v3.z, 0, 0, 1, repx, 0 ) );
		model.setVertex(4,  new Vertex( v4.x, v4.y, v4.z, 0, 0, 1, 0, 0 ) );
		model.setVertex(5,  new Vertex( v1.x, v1.y, v1.z, 0, 0, 1, 0, repy ) );


		model.setVertex(6,  new Vertex( v7.x, v7.y, v7.z, 0, 0, -1, repx, 0 ) );
		model.setVertex(7,  new Vertex( v6.x, v6.y, v6.z, 0, 0, -1, repx, repy ) );
		model.setVertex(8,  new Vertex( v5.x, v5.y, v5.z, 0, 0, -1, 0, repy ) );
		model.setVertex(9,  new Vertex( v5.x, v5.y, v5.z, 0, 0, -1, 0, repy ) );
		model.setVertex(10, new Vertex( v8.x, v8.y, v8.z, 0, 0, -1, 0, 0 ) );
		model.setVertex(11, new Vertex( v7.x, v7.y, v7.z, 0, 0, -1, repx, 0 ) );


		//
		model.setVertex(12, new Vertex( v8.x, v8.y, v8.z, -1, 0, 0, repx, repy ) );
		model.setVertex(13, new Vertex( v5.x, v5.y, v5.z, -1, 0, 0, 0, repy ) );
		model.setVertex(14, new Vertex( v1.x, v1.y, v1.z, -1, 0, 0, 0, 0 ) );
		model.setVertex(15, new Vertex( v1.x, v1.y, v1.z, -1, 0, 0, 0, 0 ) );
		model.setVertex(16, new Vertex( v4.x, v4.y, v4.z, -1, 0, 0, repx, 0 ) );
		model.setVertex(17, new Vertex( v8.x, v8.y, v8.z, -1, 0, 0, repx, repy ) );

		//
		model.setVertex(18, new Vertex( v2.x, v2.y, v2.z, 1, 0, 0, 0, 0 ) );
		model.setVertex(19, new Vertex( v6.x, v6.y, v6.z, 1, 0, 0, 0, repy ) );
		model.setVertex(20, new Vertex( v7.x, v7.y, v7.z, 1, 0, 0, repx, repy ) );
		model.setVertex(21, new Vertex( v7.x, v7.y, v7.z, 1, 0, 0, repx, repy ) );
		model.setVertex(22, new Vertex( v3.x, v3.y, v3.z, 1, 0, 0, repx, 0 ) );
		model.setVertex(23, new Vertex( v2.x, v2.y, v2.z, 1, 0, 0, 0, 0 ) );



		model.setVertex(24, new Vertex( v6.x, v6.y, v6.z, 0, -1, 0, repx, 0 ) );
		model.setVertex(25, new Vertex( v2.x, v2.y, v2.z, 0, -1, 0, repx, repy ) );
		model.setVertex(26, new Vertex( v1.x, v1.y, v1.z, 0, -1, 0, 0, repy ) );
		model.setVertex(27, new Vertex( v1.x, v1.y, v1.z, 0, -1, 0, 0, repy ) );
		model.setVertex(28, new Vertex( v5.x, v5.y, v5.z, 0, -1, 0, 0, 0 ) );
		model.setVertex(29, new Vertex( v6.x, v6.y, v6.z, 0, -1, 0, repx, 0 ) );

		model.setVertex(30, new Vertex( v4.x, v4.y, v4.z, 0, 1, 0, 0, repy ) );
		model.setVertex(31, new Vertex( v3.x, v3.y, v3.z, 0, 1, 0, repx, repy ) );
		model.setVertex(32, new Vertex( v7.x, v7.y, v7.z, 0, 1, 0, repx, 0 ) );
		model.setVertex(33, new Vertex( v7.x, v7.y, v7.z, 0, 1, 0, repx, 0 ) );
		model.setVertex(34, new Vertex( v8.x, v8.y, v8.z, 0, 1, 0, 0, 0 ) );
		model.setVertex(35, new Vertex( v4.x, v4.y, v4.z, 0, 1, 0, 0, repy ) );

		return model;
	}

	public static BufferedMesh cube( float size ) {
		return block( size, size, size );
	}

	public static BufferedMesh sphere( float radius, int steps ) {
		return sphere( radius, steps, steps );
	}

	public static BufferedMesh cylinder( float radius, float height, int steps ) {
		return cylinder( radius, radius, height, steps, 1 );
	}

	public static BufferedMesh cone( float radius, float height, int steps ) {
		return cylinder( radius, 0, height, steps, 1 );
	}

	public static BufferedMesh teapot( float radius ) {
		return TeapotData.mesh( radius );
	}
}
