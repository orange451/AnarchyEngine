package engine.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.luaj.vm2.LuaValue;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.RayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btAxisSweep3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;

import engine.Game;
import engine.InternalGameThread;

public class PhysicsWorld {
	public btDynamicsWorld dynamicsWorld;
	private List<PhysicsObjectInternal> objects = Collections.synchronizedList(new ArrayList<PhysicsObjectInternal>());

	public PhysicsWorld() {
		refresh();
	}

	public void tick() {
		float dt = 1/(float)InternalGameThread.desiredTPS;
		
		if ( Game.isRunning() && !Game.isServer() ) {
			//System.out.println("CLIENT PHYSICS! " + objects.size() + " / " + this.dynamicsWorld.getNumCollisionObjects());
		}

		synchronized(dynamicsWorld) {
			dynamicsWorld.setGravity(new Vector3(0, 0, -(Game.workspace()).get("Gravity").tofloat()));
			try{
				int reps = 4;
				float ndt = dt/(float)reps;
				
				for (int i = 0; i < reps; i++) {
					Game.runService().physicsSteppedEvent().fire(LuaValue.valueOf(ndt));
					dynamicsWorld.stepSimulation(ndt,1);
				}
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

	public PhysicsObjectInternal find( btRigidBody body ) {
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

	private static boolean initialized;
	public void refresh() {
		if (!initialized) {
			initialized = true;
			Bullet.init();
		}
		
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

	public btDynamicsWorld getDynamicsWorld() {
		return dynamicsWorld;
	}

	public ClosestRayResultCallback rayTestClosest(org.joml.Vector3f origin, org.joml.Vector3f direction) {
		Vector3 from = new Vector3( origin.x, origin.y, origin.z );
		Vector3 to   = new Vector3( origin.x + (direction.x * 1024), origin.y + (direction.y * 1024), origin.z + (direction.z * 1024) );
		RayResultCallback callback  = new ClosestRayResultCallback( from, to );
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

		btRigidBody collisionObject = physObj.body;
		Vector3 from = new Vector3( origin.x, origin.y, origin.z );
		Vector3 to   = new Vector3( origin.x + direction.x, origin.y + direction.y, origin.z + direction.z );

		// Calculate ray transforms
		Matrix4 start = new Matrix4();
		start.idt();
		start.setTranslation(from);
		Matrix4 finish = new Matrix4();
		finish.idt();
		finish.setTranslation(to);
		ClosestRayResultCallback callback = new ClosestRayResultCallback( from, to );

		// Get AABB
		Vector3 minBound = new Vector3();
		Vector3 maxBound = new Vector3();
		collisionObject.getAabb(minBound, maxBound);

		// Test if inside AABB
		boolean aabb = aabbTest( origin, direction, minBound, maxBound );

		// Perform more intensive ray test now
		if ( aabb ) {
			btCollisionWorld.rayTestSingle(start, finish, collisionObject, collisionObject.getCollisionShape(), collisionObject.getWorldTransform(), callback);
		}

		return (ClosestRayResultCallback) callback;
	}

	private boolean aabbTest(org.joml.Vector3f from, org.joml.Vector3f to, Vector3 lb, Vector3 rt) {
		org.joml.Vector3f direction = new org.joml.Vector3f(to).normalize();
		// r.dir is unit direction vector of ray
		float dirfracx = 1.0f / direction.x;
		float dirfracy = 1.0f / direction.y;
		float dirfracz = 1.0f / direction.z;
		// lb is the corner of AABB with minimal coordinates - left bottom, rt is maximal corner
		// r.org is origin of ray
		float t1 = (lb.x - from.x)*dirfracx;
		float t2 = (rt.x - from.x)*dirfracx;
		float t3 = (lb.y - from.y)*dirfracy;
		float t4 = (rt.y - from.y)*dirfracy;
		float t5 = (lb.z - from.z)*dirfracz;
		float t6 = (rt.z - from.z)*dirfracz;

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
		ClosestRayResultCallback ret = new ClosestRayResultCallback(new Vector3(origin.x, origin.y, origin.z), new Vector3(origin.x, origin.y, origin.z));
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
					if ( find((btRigidBody) c.getCollisionObject()) != null ) {
						Vector3 vec = new Vector3();
						c.getRayFromWorld(new Vector3());
						
						Vector3 hit = new Vector3();
						c.getHitPointWorld(hit);
						
						vec.sub(hit);
						
						float dist = vec.len();
						if ( dist < maxDist ) {
							maxDist = dist;
							ret = c;
						}
					} else {
						((btRigidBody)c.getCollisionObject()).dispose();
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
				object.getBody().dispose();
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
			dynamicsWorld.dispose();
		
		// Make sure objects list is clear
		objects.clear();
	}
	
	public void cleanup() {
		destroy();

		// Create new Dynamics World
		btCollisionConfiguration collisionConfiguration = new btDefaultCollisionConfiguration();
		btCollisionDispatcher dispatcher = new btCollisionDispatcher(collisionConfiguration);
		btConstraintSolver solver = new btSequentialImpulseConstraintSolver();
		Vector3 worldMin = new Vector3(-1000f,-1000f,-1000f);
		Vector3 worldMax = new Vector3(1000f,1000f,1000f);
		btAxisSweep3 sweepBP = new btAxisSweep3(worldMin, worldMax);
		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, sweepBP, solver, collisionConfiguration);
		
		//btGImpactCollisionAlgorithm.registerAlgorithm( dispatcher );
	}
}
