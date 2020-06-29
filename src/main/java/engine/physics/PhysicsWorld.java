/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.luaj.vm2.LuaValue;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.RayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btGImpactCollisionAlgorithm;
import com.badlogic.gdx.physics.bullet.collision.btGhostPairCallback;
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
	private Map<btRigidBody, PhysicsObjectInternal> btToInternal = Collections.synchronizedMap(new HashMap<>());

	public PhysicsWorld() {
		refresh();
	}

	public void tick() {
		float fixedDelta = 1f/InternalGameThread.desiredTPS;
		float delta = InternalGameThread.delta;

		if ( Game.isRunning() ) {
			synchronized(dynamicsWorld) {
				dynamicsWorld.setGravity(new Vector3(0, 0, -Game.workspace().getGravity()));
				try{
					Game.runService().physicsSteppedEvent().fire(LuaValue.valueOf(delta));
					dynamicsWorld.stepSimulation(Math.min(1f / 30f, delta), 5, fixedDelta);
				}catch(Exception e) {
					System.err.println("Error Stepping Physics");
					e.printStackTrace();
	
					if ( e instanceof NullPointerException || e instanceof ArrayIndexOutOfBoundsException ) {
						//GameEngine.game_end();
					}
				}
			}
		}
		
		//com.badlogic.gdx.physics.bullet.dynamics.btFixedConstraint

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

	/*public PhysicsObjectInternal find( btRigidBody body ) {
		synchronized(objects) {
			for (int i = 0; i < objects.size(); i++) {
				PhysicsObjectInternal obj = objects.get(i);
				if ( obj.body.equals(body) ) {
					return obj;
				}
			}
		}
		return null;
	}*/

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
		btToInternal.clear();

		cleanup();

		// Re add objects
		for (int i = 0; i < temp.size(); i++)
			add( temp.get(i) );
	}

	protected void add(final PhysicsObjectInternal obj) {
		synchronized(objects) {
			objects.add(obj);
			
			InternalGameThread.runLater(() -> {
				if ( !obj.destroyed ) {
					dynamicsWorld.addRigidBody(obj.getBody());
					btToInternal.put(obj.getBody(), obj);
				}
			});
		}
	}

	public btDynamicsWorld getDynamicsWorld() {
		return dynamicsWorld;
	}
	
	public PhysicsObjectInternal getPhysicsObject(btRigidBody body) {
		return btToInternal.get(body);
	}

	public ClosestRayResultCallback rayTestClosest(org.joml.Vector3f origin, org.joml.Vector3f direction) {
		Vector3 from = new Vector3( origin.x, origin.y, origin.z );
		Vector3 to   = new Vector3( origin.x + direction.x, origin.y + direction.y, origin.z + direction.z );
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
	private Vector3 tempMinBound = new Vector3();
	private Vector3 tempMaxBound = new Vector3();
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
		collisionObject.getAabb(tempMinBound, tempMaxBound);
		boolean aabb = aabbTest( origin, direction, tempMinBound, tempMaxBound );

		// Perform more intensive ray test now
		if ( aabb ) {
			btCollisionWorld.rayTestSingle(start, finish, collisionObject, collisionObject.getCollisionShape(), collisionObject.getWorldTransform(), callback);
		}

		return callback;
	}

	private volatile org.joml.Vector3f tempDirection;
	private boolean aabbTest(org.joml.Vector3f from, org.joml.Vector3f to, Vector3 lb, Vector3 rt) {
		if ( tempDirection == null )
			tempDirection = new org.joml.Vector3f(to).normalize();
		else
			tempDirection.set(to).normalize();
		
		// r.dir is unit direction vector of ray
		float dirfracx = 1.0f / tempDirection.x;
		float dirfracy = 1.0f / tempDirection.y;
		float dirfracz = 1.0f / tempDirection.z;
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

	private List<PhysicsObjectInternal> tempExclusionList = new ArrayList<>();
	public ClosestRayResultCallback rayTestExcluding( org.joml.Vector3f origin, org.joml.Vector3f direction, PhysicsObjectInternal... excluding ) {
		ClosestRayResultCallback ret = new ClosestRayResultCallback(new Vector3(origin.x, origin.y, origin.z), new Vector3(origin.x, origin.y, origin.z));
		synchronized(tempExclusionList) {
			
			// Add to exclusion list
			tempExclusionList.clear();
			for (int j = 0; j < excluding.length; j++) {
				tempExclusionList.add(excluding[j]);
			}
			
			// Test each object
			float maxDist = Float.MAX_VALUE;
			synchronized(objects) {
				checkObject: for (int i = 0; i < objects.size(); i++) {
					PhysicsObjectInternal obj = objects.get(i);
	
					// Check if we exclude this object from the ray-test
					for (int j = 0; j < tempExclusionList.size(); j++) {
						if ( tempExclusionList.get(j).equals(obj) ) {
							tempExclusionList.remove(j);
							continue checkObject;
						}
					}
	
					// Raytest
					ClosestRayResultCallback c = rayTest( origin, direction, obj );
					if ( c.hasHit() ) {
						if ( btToInternal.containsKey((btRigidBody) c.getCollisionObject()) ) {
							Vector3 vec = new Vector3();
							c.getRayFromWorld(new Vector3());
							
							Vector3 hit = new Vector3();
							c.getHitPointWorld(hit);
							
							vec.sub(hit);
							
							float dist = vec.len();
							if ( dist < maxDist ) {
								maxDist = dist;
								ret.dispose();
								ret = c;
							} else {
								c.dispose();
							}
						} else {
							c.getCollisionObject().dispose();
							c.dispose();
						}
					} else {
						c.dispose();
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
				btToInternal.remove(object.getBody());
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
		if ( dynamicsWorld != null && !dynamicsWorld.isDisposed() )
			dynamicsWorld.dispose();
		
		// Make sure objects list is clear
		objects.clear();
		btToInternal.clear();
	}
	
	public void cleanup() {
		destroy();

		// Create new Dynamics World
		btCollisionConfiguration collisionConfiguration = new btDefaultCollisionConfiguration();
		btCollisionDispatcher dispatcher = new btCollisionDispatcher(collisionConfiguration);
		btConstraintSolver solver = new btSequentialImpulseConstraintSolver();
		btBroadphaseInterface overlappingPairCache = new btDbvtBroadphase();
		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, overlappingPairCache, solver, collisionConfiguration);
		
		// Register GIMPACT collision algo
		btGImpactCollisionAlgorithm.registerAlgorithm( dispatcher );
		
		// Allow ghost objects
		dynamicsWorld.getBroadphase().getOverlappingPairCache().setInternalGhostPairCallback(new btGhostPairCallback());
	}
}
