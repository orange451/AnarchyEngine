package engine.physics;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShapeZ;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShapeZ;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import engine.Game;
import engine.InternalGameThread;
import engine.gl.mesh.BufferedMesh;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.services.Workspace;
import engine.util.Pair;
import engine.util.PhysicsUtils;

public class PhysicsObjectInternal {
	protected btRigidBody body;
	protected PhysicsWorld world;
	protected boolean destroyed;
	protected SoundMaterial soundMaterial;
	private float desiredMass = 0.5f;
	private float desiredBounce = 0.5f;
	private float desiredFriction = 0.5f;
	private float desiredAngularFactor = 1.0f;
	private float desiredLinearDamping = 0.1f;
	private btCollisionShape desiredShape;
	private boolean refresh = false;
	private PhysicsBase luaFrontEnd;
	
	private Vector3 forceVelocity;
	
	public PhysicsObjectInternal( PhysicsBase physicsObject ) {
		this.luaFrontEnd = physicsObject;
		
		// Default values
		float mass = luaFrontEnd.rawget("Mass").tofloat();
		float bounciness = luaFrontEnd.rawget("Bounciness").tofloat();
		float friction = luaFrontEnd.rawget("Friction").tofloat();
		float angular = luaFrontEnd.rawget("AngularFactor").tofloat();
		float linearD = luaFrontEnd.rawget("LinearDamping").tofloat();
		this.desiredBounce = bounciness;
		this.desiredFriction = friction;
		this.desiredMass = mass;
		this.desiredAngularFactor = angular;
		this.desiredLinearDamping = linearD;
		
		// Create body
		this.refreshBody();
	}

	public void setSoundMaterial(SoundMaterial material) {
		this.soundMaterial = material;
	}

	public SoundMaterial getSoundMaterial() {
		return soundMaterial;
	}

	public btRigidBody getBody() {
		return this.body;
	}

	public Vector3f getVelocity() {
		Vector3 vel = getVelocityInternal();
		return new Vector3f( vel.x, vel.y, vel.z );
	}

	public Vector3 getVelocityInternal() {
		if (this.destroyed || body == null)
			return new Vector3();
		
		return body.getLinearVelocity();
	}
	
	public Vector3f getAngularVelocity() {
		Vector3 vel = this.getAngularVelocityInternal();
		return new Vector3f( vel.x, vel.y, vel.z );
	}
	
	public Vector3 getAngularVelocityInternal() {
		if ( this.destroyed || body == null )
			return new Vector3();
		
		return body.getAngularVelocity();
	}
	
	public void setAngularVelocity(Vector3f vector) {
		setAngularVelocity( new Vector3(vector.x, vector.y, vector.z) );
	}
	
	public void setAngularVelocity(Vector3 vector) {
		if ( this.destroyed )
			return;
		
		if ( body == null )
			return;
		
		body.activate(true);
		body.setAngularVelocity(vector);
	}

	public Vector3f getLocation() {
		if (this.destroyed)
			return new Vector3f(0,0,0);
		com.badlogic.gdx.math.Matrix4 transform = body.getWorldTransform();
		Vector3 capPos = transform.getTranslation(new Vector3());
		return new Vector3f( capPos.x, capPos.y, capPos.z );
	}

	public void setVelocity( Vector3f vector ) {
		this.setVelocity( new Vector3( vector.x, vector.y, vector.z ) );
	}

	public void setVelocity( Vector3 vector ) {
		if (this.destroyed)
			return;
		
		if ( body == null ) {
			forceVelocity = vector;
			return;
		}
		
		Vector3 oldVel = body.getLinearVelocity();
		oldVel.sub(vector);
		float distSqr = oldVel.len2();
		
		if ( distSqr > 0.05 )
			this.wakeup();

		body.activate(true);
		body.setLinearVelocity( vector );
	}

	public void applyImpulse( Vector3f impulse, Vector3f location ) {
		if (this.destroyed)
			return;
		body.applyImpulse( new Vector3( impulse.x, impulse.y, impulse.z ), new Vector3( location.x, location.y, location.z ));
	}

	private Matrix4 transform = new Matrix4();
	private float[] transformTemp = new float[16];
	public void setWorldMatrix( Matrix4f worldMatrix ) {
		if (this.destroyed || body == null)
			return;
		transform.set(worldMatrix.get(transformTemp));
		//transform.setFromOpenGLMatrix(worldMatrix.get(new float[16]));
		body.setWorldTransform(transform);
		this.wakeup();
	}

	public void teleport( Vector3f pos ) {
		if (this.destroyed)
			return;

		Matrix4 transform = body.getWorldTransform();
		transform.setTranslation(pos.x, pos.y, pos.z);
		body.setWorldTransform(transform);
	}

	private static final LuaValue C_LINKED = LuaValue.valueOf("Linked");
	private static final LuaValue C_WORLDMATRIX = LuaValue.valueOf("WorldMatrix");
	private static final LuaValue C_PREFAB = LuaValue.valueOf("Prefab");

	private static final Matrix4f IDENTITY = new Matrix4f().identity();
	
	private Matrix4f tempWorldMat = new Matrix4f();
	
	public Matrix4f getWorldMatrix() {
		if (this.destroyed || body == null) {
			if ( luaFrontEnd == null )
				return IDENTITY;
			
			LuaValue linked = luaFrontEnd.get(C_LINKED);
			if ( linked.isnil() )
				return IDENTITY;
			
			LuaValue wMat = linked.get(C_WORLDMATRIX);
			if ( wMat.isnil() )
				return IDENTITY;
			
			return ((engine.lua.type.data.Matrix4)wMat).getInternal();
		}

		Matrix4 transform = getWorldMatrixInternal();
		tempWorldMat.set(transform.val);

		return tempWorldMat;
	}
	
	public Matrix4 getWorldMatrixInternal() {
		if ( this.destroyed || body == null )
			return new Matrix4();
		
		return this.body.getWorldTransform();
	}
	
	private Quaternionf tempRotation = new Quaternionf();

	public Quaternionf getRotation() {
		return getWorldMatrix().getNormalizedRotation(tempRotation);
	}

	public void destroy() {
		if ( world != null ) {
			this.world.destroy(this);
		}
		this.destroyed = true;
	}

	public void setMass(float mass) {
		this.desiredShape = null; // Force shape to change next refresh
		
		if ( this.body != null ) {
			this.body.setMassProps(mass, this.body.getLocalInertia());
		} else {
			this.refresh = true;
		}
	}
	
	public void setFriction(float friction) {
		this.desiredFriction = friction;
		
		if ( this.body != null ) {
			this.body.setFriction(friction);
		} else {
			this.refresh = true;
		}
	}
	
	public void setAngularFactor( float desiredAngularFactor ) {
		this.desiredAngularFactor = desiredAngularFactor;

		if ( this.body != null ) {
			this.body.setAngularFactor(desiredAngularFactor);
		} else {
			this.refresh = true;
		}
	}
	
	public void setLinearDamping( float desiredLinearDamping ) {
		this.desiredLinearDamping = desiredLinearDamping;
		
		if ( this.body != null ) {
			this.body.setDamping(desiredLinearDamping, 0.3f);
		} else {
			this.refresh = true;
		}
	}

	public void setBounciness(float bounciness) {
		this.desiredBounce = bounciness;
		
		if ( this.body != null ) {
			this.body.setRestitution(bounciness);
		} else {
			this.refresh = true;
		}
	}

	private void refreshBody() {
		refresh = false;
		
		Matrix4f worldMat = this.getWorldMatrix();
		final Vector3 vel = this.getVelocityInternal();
		Vector3 angVel = this.getAngularVelocityInternal();
		final float newMass = desiredMass;
		btRigidBody old = body;
		
		InternalGameThread.runLater(() -> {
			if ( luaFrontEnd.isDestroyed() )
				return;
			refresh = false;
			
			// Shape
			if ( desiredShape == null ) {
				if ( luaFrontEnd.rawget("UseCustomMesh").checkboolean() ) {
					setShapeFromMesh(luaFrontEnd.rawget("CustomMesh"));
				} else {
					setShapeFromType(luaFrontEnd.rawget("Shape").toString());
				}
			}
			
			// Destroy old
			if ( old != null && world != null ) {
				world.dynamicsWorld.removeRigidBody(old);
				old.dispose();
			}
			
			// Create new
			btRigidBody newBody = PhysicsUtils.getBody(newMass, desiredBounce, desiredFriction, desiredShape);
			newBody.setAngularFactor(desiredAngularFactor);
			newBody.setDamping(desiredLinearDamping, 0.3f);
			
			// Set the body
			PhysicsObjectInternal.this.body = newBody;
			
			// Check for force setting velocity
			if ( forceVelocity != null ) {
				vel.set(forceVelocity);
				forceVelocity = null;
			} else {
				engine.lua.type.data.Vector3 v1 = luaFrontEnd.getVelocity();
				vel.set(v1.getX(), v1.getY(), v1.getZ());
			}
			
			// Set angular velocity
			engine.lua.type.data.Vector3 v2 = luaFrontEnd.getAngularVelocity();
			angVel.set(v2.getX(), v2.getY(), v2.getZ());
			
			// Set some vars
			setWorldMatrix(worldMat);
			newBody.setHitFraction(0);
			newBody.setCcdMotionThreshold((float) 1e-7);
			newBody.setCcdSweptSphereRadius((float) 1e-2);
			
			// Add to world
			Workspace workspace = Game.workspace();
			if ( workspace != null && world == null ) {
				this.world = workspace.getPhysicsWorld();
				this.world.add(this);
			} else {
				this.world.dynamicsWorld.addRigidBody(newBody);
			}
			
			this.wakeup();
			newBody.activate(true);
			newBody.setLinearVelocity(vel);
			newBody.setAngularVelocity(angVel);
		});
	}

	public void internalTick() {
		if ( luaFrontEnd == null )
			return;
		
		if ( this.refresh && luaFrontEnd.isDescendantOf(Game.workspace()) ) {
			refreshBody();
		}
	}
	
	public void setShapeFromMesh(LuaValue customMesh) {
		if ( luaFrontEnd == null )
			return;
		
		if ( luaFrontEnd.get(C_LINKED).isnil() || luaFrontEnd.get(C_LINKED).get(C_PREFAB).isnil() ) {
			setShapeFromType("Box");
			return;
		}
		
		float scale = 1.0f;
		Prefab prefab = (Prefab) luaFrontEnd.get(C_LINKED).get(C_PREFAB);
		if ( !prefab.isnil() ) {
			scale = prefab.getScale();
		}
		
		BufferedMesh mesh = null;
		if ( customMesh.isnil() ) {
			mesh = prefab.getPrefab().getCombinedMesh();
		} else {
			mesh = ((Mesh)customMesh).getMesh();
		}
		
		if ( desiredMass == 0 ) {
			desiredShape = PhysicsUtils.meshShapeStatic(mesh, scale );
		} else {
			desiredShape = PhysicsUtils.meshShapeDynamic(mesh, scale);
		}
	}
	
	public void setShapeFromType(String type) {
		if ( luaFrontEnd == null )
			return;
		
		float width = 1;
		float length = 1;
		float height = 1;
		Pair<Vector3f, Vector3f> aabb = luaFrontEnd.getAABB();
		if ( aabb != null ) {
			width  = aabb.value2().x-aabb.value1().x;
			length = aabb.value2().y-aabb.value1().y;
			height = aabb.value2().z-aabb.value1().z;
		}
		float minlen = Math.min(width, Math.min(length, height));
		float minrad = Math.min(width, length);
		Vector3 halfExtents = new Vector3(width/2f, length/2f, height/2f);
		
		// Fallback shape
		btCollisionShape shape = new btBoxShape(halfExtents);
		
		// Sphere
		if ( type.equals("Sphere") ) {
			shape = new btSphereShape(minlen/2f);
		}
		
		// Capsule
		if ( type.equals("Capsule") ) {
			shape = new btCapsuleShapeZ(minrad/2f, height-minrad);
		}
		
		// Cylinder
		if ( type.equals("Cylinder") ) {
			shape = new btCylinderShapeZ(halfExtents);
		}
		
		// Hull
		if ( type.equals("Hull") && !luaFrontEnd.get(C_LINKED).get(C_PREFAB).isnil() ) {
			Prefab prefab = (Prefab) luaFrontEnd.get(C_LINKED).get(C_PREFAB);
			BufferedMesh mesh = prefab.getPrefab().getCombinedMesh();
			shape = PhysicsUtils.hullShape(mesh, 1.0f, mesh.getSize() > 64);
		}
		
		this.desiredShape = shape;
	}

	public boolean isDestroyed() {
		return this.destroyed;
	}

	public void wakeup() {
		if ( this.body == null || this.destroyed )
			return;
		
		this.body.setActivationState(1); // ACTIVE_TAG
		this.body.activate();
		this.body.activate(true);
	}

	/**
	 * Force the physics object to refresh
	 */
	public void refresh() {
		this.refresh = true;
		this.desiredShape = null;
	}
}
