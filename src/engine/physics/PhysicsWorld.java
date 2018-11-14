package engine.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback;
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.extras.gimpact.GImpactCollisionAlgorithm;
import com.bulletphysics.linearmath.Transform;

import engine.Game;
import engine.InternalGameThread;

public class PhysicsWorld {
	public DynamicsWorld dynamicsWorld;
	private List<PhysicsObjectInternal> objects = Collections.synchronizedList(new ArrayList<PhysicsObjectInternal>());

	public PhysicsWorld() {
		refresh();
	}

	public void tick() {
		float dt = InternalGameThread.delta;
		
		if ( Game.isRunning() && !Game.isServer() ) {
			//System.out.println("CLIENT PHYSICS! " + objects.size() + " / " + this.dynamicsWorld.getNumCollisionObjects());
		}

		synchronized(dynamicsWorld) {
			dynamicsWorld.setGravity(new Vector3f(0, 0, -(Game.workspace()).get("Gravity").tofloat()));
			try{
				dynamicsWorld.stepSimulation(dt,2);
			}catch(Exception e) {
				System.err.println("Error Stepping Physics");
				e.printStackTrace();

				if ( e instanceof NullPointerException || e instanceof ArrayIndexOutOfBoundsException ) {
					//GameEngine.game_end();
				}
			}
		}

		synchronized(objects) {
			for (int i = 0; i < objects.size(); i++) {
				if ( i >= objects.size() )
					continue;
				PhysicsObjectInternal obj = objects.get(i);
				if ( obj == null )
					continue;
				obj.internalTick();
			}
		}
	}

	public PhysicsObjectInternal find( RigidBody body ) {
		synchronized(objects) {
			for (int i = 0; i < objects.size(); i++) {
				PhysicsObjectInternal obj = objects.get(i);
				if ( obj.body.equals(body) ) {
					return obj;
				}
			}
		}
		return null;
	}

	public void refresh() {
		// Create clone of objects list
		List<PhysicsObjectInternal> temp = Collections.synchronizedList(new ArrayList<PhysicsObjectInternal>());
		for (int i = 0; i < objects.size(); i++) {
			PhysicsObjectInternal obj = objects.get(i);
			if ( obj == null || obj.destroyed )
				continue;
			temp.add(obj);
		}
		objects.clear();

		cleanup();

		// Re add objects
		for (int i = 0; i < temp.size(); i++)
			add( temp.get(i) );
	}

	protected void add(final PhysicsObjectInternal obj) {
		synchronized(objects) {
			objects.add(obj);
			
			Game.runLater(() -> {
				if ( !obj.destroyed )
					dynamicsWorld.addRigidBody(obj.getBody());
			});
		}
	}

	public DynamicsWorld getDynamicsWorld() {
		return dynamicsWorld;
	}

	public ClosestRayResultCallback rayTestClosest(org.joml.Vector3f origin, org.joml.Vector3f direction) {
		javax.vecmath.Vector3f from = new javax.vecmath.Vector3f( origin.x, origin.y, origin.z );
		javax.vecmath.Vector3f to   = new javax.vecmath.Vector3f( origin.x + (direction.x * 1024), origin.y + (direction.y * 1024), origin.z + (direction.z * 1024) );
		RayResultCallback callback  = new CollisionWorld.ClosestRayResultCallback( from, to );
		dynamicsWorld.rayTest(from, to, callback);

		return (ClosestRayResultCallback) callback;
	}

	/**
	 * Performs a ray test against a specific physics object
	 * @param origin
	 * @param direction
	 * @param physObj
	 * @return
	 */
	public ClosestRayResultCallback rayTest( org.joml.Vector3f origin, org.joml.Vector3f direction, PhysicsObjectInternal physObj ) {

		RigidBody collisionObject = physObj.body;
		javax.vecmath.Vector3f from = new javax.vecmath.Vector3f( origin.x, origin.y, origin.z );
		javax.vecmath.Vector3f to   = new javax.vecmath.Vector3f( origin.x + (direction.x * 512) + 0.01f, origin.y + (direction.y * 512), origin.z + (direction.z * 512) );

		// Calculate ray transforms
		Transform start = new Transform();
		start.setIdentity();
		start.origin.set( from );
		Transform finish = new Transform();
		finish.setIdentity();
		finish.origin.set( to );
		ClosestRayResultCallback callback = new CollisionWorld.ClosestRayResultCallback( from, to );

		// Get AABB
		Vector3f minBound = new javax.vecmath.Vector3f();
		Vector3f maxBound = new javax.vecmath.Vector3f();
		collisionObject.getAabb(minBound, maxBound);

		// Test if inside AABB
		boolean aabb = aabbTest( from, direction, minBound, maxBound );

		// Perform more intensive ray test now
		if ( aabb ) {
			CollisionWorld.rayTestSingle(start, finish, collisionObject, collisionObject.getCollisionShape(), collisionObject.getWorldTransform(new Transform()), callback);
		}

		return (ClosestRayResultCallback) callback;
	}

	private boolean aabbTest(Vector3f from, org.joml.Vector3f direction, Vector3f lb, Vector3f rt) {
		Vector3f dirfrac = new Vector3f();
		// r.dir is unit direction vector of ray
		dirfrac.x = 1.0f / direction.x;
		dirfrac.y = 1.0f / direction.y;
		dirfrac.z = 1.0f / direction.z;
		// lb is the corner of AABB with minimal coordinates - left bottom, rt is maximal corner
		// r.org is origin of ray
		float t1 = (lb.x - from.x)*dirfrac.x;
		float t2 = (rt.x - from.x)*dirfrac.x;
		float t3 = (lb.y - from.y)*dirfrac.y;
		float t4 = (rt.y - from.y)*dirfrac.y;
		float t5 = (lb.z - from.z)*dirfrac.z;
		float t6 = (rt.z - from.z)*dirfrac.z;

		float tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
		float tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
		@SuppressWarnings("unused")
		float t;

		// if tmax < 0, ray (line) is intersecting AABB, but the whole AABB is behind us
		if (tmax < 0)
		{
		    t = tmax;
		    return false;
		}

		// if tmin > tmax, ray doesn't intersect AABB
		if (tmin > tmax)
		{
		    t = tmax;
		    return false;
		}

		t = tmin;
		return true;
	}

	public ClosestRayResultCallback rayTestExcluding( org.joml.Vector3f origin, org.joml.Vector3f direction, PhysicsObjectInternal... excluding ) {
		//ArrayList<ClosestRayResultCallback> callbacks = new ArrayList<ClosestRayResultCallback>();
		float maxDist = Float.MAX_VALUE;
		ClosestRayResultCallback ret = null;
		synchronized(objects) {
			for (int i = 0; i < objects.size(); i++) {
				PhysicsObjectInternal obj = objects.get(i);

				// Check if we exclude this object from the ray-test
				boolean exclude = false;
				if ( excluding != null ) {
					for (int j = 0; j < excluding.length && !exclude; j++) {
						if ( excluding[j].equals(obj) ) {
							exclude = true;
						}
					}
					// If we exclude it. Do not ray-test
					if ( exclude )
						continue;
				}

				// Raytest
				ClosestRayResultCallback c = rayTest( origin, direction, obj );
				if ( c.hasHit() ) {
					if ( find((RigidBody) c.collisionObject) != null ) {
						Vector3f vec = new Vector3f(c.rayFromWorld);
						vec.sub(c.hitPointWorld);
						float dist = vec.length();
						if ( dist < maxDist ) {
							maxDist = dist;
							ret = c;
						}
					} else {
						((RigidBody)c.collisionObject).destroy();
					}
				}
			}
		}

		return ret;
	}

	public void destroy(final PhysicsObjectInternal object) {
		object.destroyed = true;
		try {
			synchronized(dynamicsWorld) {
				dynamicsWorld.removeRigidBody(object.getBody());
				object.getBody().destroy();
			}
		}catch(Exception e ) {
			e.printStackTrace();
		}
		synchronized(objects) {
			objects.remove(object);
		}
	}
	
	public void destroy() {
		// Destroy old objects
		synchronized(objects) {
			for (int i = objects.size()-1; i >= 0; i--) {
				objects.get(i).destroy();
			}
		}
		
		// Destroy old dynamics world
		if ( dynamicsWorld != null )
			dynamicsWorld.destroy();
		
		// Make sure objects list is clear
		objects.clear();
	}
	
	public void cleanup() {
		destroy();

		// Create new Dynamics World
		CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
		CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
		ConstraintSolver solver = new SequentialImpulseConstraintSolver();
		Vector3f worldMin = new Vector3f(-1000f,-1000f,-1000f);
		Vector3f worldMax = new Vector3f(1000f,1000f,1000f);
		AxisSweep3 sweepBP = new AxisSweep3(worldMin, worldMax);
		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, sweepBP, solver, collisionConfiguration);
		GImpactCollisionAlgorithm.registerAlgorithm( getDispatcher() );
	}

	public CollisionDispatcher getDispatcher() {
		return (CollisionDispatcher) this.dynamicsWorld.getDispatcher();
	}
}
