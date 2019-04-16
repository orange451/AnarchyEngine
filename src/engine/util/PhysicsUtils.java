package engine.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CapsuleShapeZ;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.collision.shapes.CylinderShapeZ;
import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.collision.shapes.ShapeHull;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.extras.gimpact.GImpactMeshShape;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;

import engine.gl.mesh.BufferedMesh;
import engine.gl.mesh.Vertex;

public class PhysicsUtils {
	private static Transform DEFAULT_TRANSFORM = new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), new Vector3f(0, 0, 0), 1.0f));
	
	public static RigidBody box( float mass, float bouncyness, float width, float length, float height ) {
		ConvexShape boxShape = new BoxShape(new Vector3f(width/2f, length/2f, height/2f));
		return getBody( mass, bouncyness, 0.5f, boxShape );
	}

	public static RigidBody cube( float mass, float bouncyness, float length ) {
		return box( mass, bouncyness, length, length, length );
	}

	public static RigidBody sphere( float mass, float bouncyness, float radius ) {
		ConvexShape ballShape = new SphereShape(radius);
		return getBody( mass, bouncyness, 0.5f, ballShape );
	}

	public static RigidBody capsule( float mass, float bouncyness, float radius, float height ) {
		ConvexShape shape = new CapsuleShapeZ( radius, (height*0.99f) - radius*2 );
		return getBody( mass, bouncyness, 0.5f, shape );
	}

	public static RigidBody cylinder( float mass, float bouncyness, float radius, float height ) {
		ConvexShape shape = new CylinderShapeZ( new Vector3f(radius, radius, height ) );
		return getBody( mass, bouncyness, 0.5f, shape );
	}

	public static RigidBody shape( float mass, float bouncyness, ConvexShape shape ) {
		return getBody( mass, bouncyness, 0.5f, shape );
	}

	public static RigidBody getBody( float mass, float bouncyness, float friction, CollisionShape shape ) {
		MotionState bodyMotionState = new DefaultMotionState(DEFAULT_TRANSFORM);
		Vector3f ballInertia = new Vector3f();
		if ( mass != 0 )
			shape.calculateLocalInertia(mass, ballInertia);
		
		RigidBodyConstructionInfo bodyInfo = new RigidBodyConstructionInfo(mass, bodyMotionState, shape, ballInertia);
		bodyInfo.restitution = bouncyness;
		bodyInfo.friction = friction;
		bodyInfo.angularDamping = 0.3f;
		bodyInfo.linearDamping = 0.1f;

		RigidBody body = new RigidBody(bodyInfo);
		if ( mass == 0 )
			body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
		body.activate(true);

		return body;
	}

	/**
	 * Will return a rigid-body based on a mesh. It is automatically added to the physics world.
	 * <br>
	 * <br>
	 * Should not be used for dynamic physics objects.
	 * @param mass
	 * @param bouncyness
	 * @param bufferedMesh
	 * @return
	 */
	public static RigidBody mesh(float mass, float bouncyness, float friction, BufferedMesh bufferedMesh) {
		if ( mass == 0 ) {
			return getBody( mass, bouncyness, friction, meshShapeStatic( bufferedMesh, 1.0f ) );
		} else {
	        return getBody( mass, bouncyness, friction, meshShapeDynamic( bufferedMesh, 1.0f ) );
		}
	}
	
	public static CollisionShape meshShapeStatic( BufferedMesh bufferedMesh, float scale ) {
		if ( bufferedMesh == null )
			return null;
		
		return new BvhTriangleMeshShape(meshVertexArray(bufferedMesh, scale), true);
	}
	
	public static CollisionShape meshShapeDynamic( BufferedMesh bufferedMesh, float scale ) {
		if ( bufferedMesh == null )
			return null;

		GImpactMeshShape meshShape = new GImpactMeshShape(meshVertexArray(bufferedMesh, scale));
		meshShape.updateBound();
		
		return meshShape;
	}
	
	private static TriangleIndexVertexArray meshVertexArray( BufferedMesh bufferedMesh, float scale ) {
		if ( bufferedMesh == null )
			return null;
		
		//System.out.println(bufferedMesh.getSize());
		int totalVerts = bufferedMesh.getSize();
		int totalTris = totalVerts / 3;
		ByteBuffer gVertices = ByteBuffer.allocateDirect(totalVerts * 3 * 4).order(ByteOrder.nativeOrder());
		ByteBuffer gIndices = ByteBuffer.allocateDirect(totalTris * 3 * 4).order(ByteOrder.nativeOrder());

		// FILL VERTEX AND INDEX DATA
		Vertex[] vertices = bufferedMesh.getVertices();
		for (int i = 0; i < vertices.length; i++) {
			Vertex vertex = vertices[i];
			float[] pos = vertex.getXYZ();
			gVertices.putFloat(pos[0]);
			gVertices.putFloat(pos[1]);
			gVertices.putFloat(pos[2]);

			gIndices.putInt(i);
		}
		gVertices.rewind();
		gIndices.rewind();
		
		// Create mesh
		IndexedMesh indexedMesh = new IndexedMesh();
		indexedMesh.numVertices = totalVerts;
		indexedMesh.vertexBase = gVertices;
		indexedMesh.vertexStride = 3 * 4;
		indexedMesh.numTriangles = totalTris;
		indexedMesh.triangleIndexBase = gIndices;
		indexedMesh.triangleIndexStride = 3 * 4;

		TriangleIndexVertexArray vertArray = new TriangleIndexVertexArray();
		vertArray.addIndexedMesh(indexedMesh);
		
		return vertArray;
	}

	/**
	 * Returns a rigidbody based on a simplified version of the supplied mesh. It is automatically added to the physics world.
	 * <br>
	 * <br>
	 * Can be used with dynamic physics objects.
	 * @param mass
	 * @param bouncyness
	 * @param mesh
	 * @return
	 */
	public static RigidBody hull( float mass, float bouncyness, float friction, float scalar, BufferedMesh mesh, boolean simplified ) {
		// REturn rigid body
		return getBody( mass, bouncyness, friction, hullShape(mesh, scalar, simplified) );
	}
	
	public static ConvexShape hullShape( BufferedMesh mesh, float scale, boolean simplified ) {
		if ( mesh == null ) {
			return new BoxShape( new Vector3f(0.5f, 0.5f, 0.5f) );
		}
		// Populate vertex list
		ObjectArrayList<Vector3f> vertices = new ObjectArrayList<Vector3f>();
		for (int i = 0; i < mesh.getSize(); i++) {
			Vertex vert = mesh.getVertex(i);
			float[] xyz = vert.getXYZ();
			vertices.add( new Vector3f( xyz[0] * scale, xyz[1] * scale, xyz[2] * scale ) );
		}

		// Create original hull from vertex list
		ConvexShape shape = new ConvexHullShape(vertices);

		// Create simplified hull
		if ( simplified ) {
			ShapeHull hull = new ShapeHull(shape);
			hull.buildHull(shape.getMargin());
			shape = new ConvexHullShape(hull.getVertexPointer());
		}
		
		return shape;
	}
}
