package engine.physics;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import engine.Game;
import engine.InternalGameThread;
import engine.gl.mesh.BufferedMesh;
import engine.lua.type.data.Matrix4;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.services.Workspace;
import engine.util.Pair;
import engine.util.PhysicsUtils;

public class PhysicsObjectInternal {
	protected RigidBody body;
	protected PhysicsWorld world;
	protected boolean destroyed;
	protected SoundMaterial soundMaterial;
	private float desiredMass = 0.5f;
	private float desiredBounce = 0.5f;
	private float desiredFriction = 0.5f;
	private float desiredAngularFactor = 1.0f;
	private float desiredLinearDamping = 0.1f;
	private CollisionShape desiredShape;
	private boolean refresh = false;
	private PhysicsBase luaFrontEnd;
	
	private javax.vecmath.Vector3f forceVelocity;
	
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

	public RigidBody getBody() {
		return this.body;
	}

	public Vector3f getVelocity() {
		javax.vecmath.Vector3f vel = getVelocityInternal();
		return new Vector3f( vel.x, vel.y, vel.z );
	}

	public javax.vecmath.Vector3f getVelocityInternal() {
		if (this.destroyed || body == null)
			return new javax.vecmath.Vector3f();
		
		return body.getLinearVelocity(new javax.vecmath.Vector3f());
	}

	public Vector3f getLocation() {
		if (this.destroyed)
			return new Vector3f(0,0,0);
		Transform transform = body.getWorldTransform(new Transform());
		javax.vecmath.Vector3f capPos = transform.origin;
		return new Vector3f( capPos.x, capPos.y, capPos.z );
	}

	public void setVelocity( Vector3f vector ) {
		this.setVelocity( new javax.vecmath.Vector3f( vector.x, vector.y, vector.z ) );
	}

	public void setVelocity( javax.vecmath.Vector3f vector ) {
		if (this.destroyed)
			return;
		
		if ( body == null ) {
			forceVelocity = vector;
			return;
		}
		
		javax.vecmath.Vector3f oldVel = body.getLinearVelocity(new javax.vecmath.Vector3f());
		oldVel.sub(vector);
		float distSqr = oldVel.lengthSquared();
		
		if ( distSqr > 0.1 )
			this.wakeup();
		
		body.setLinearVelocity( vector );
	}

	public void applyImpulse( Vector3f impulse, Vector3f location ) {
		if (this.destroyed)
			return;
		body.applyImpulse( new javax.vecmath.Vector3f( impulse.x, impulse.y, impulse.z ), new javax.vecmath.Vector3f( location.x, location.y, location.z ));
	}

	private Transform transform = new Transform();
	public void setWorldMatrix( Matrix4f worldMatrix ) {
		if (this.destroyed || body == null)
			return;
		transform.setFromOpenGLMatrix(worldMatrix.get(new float[16]));
		body.setWorldTransform(transform);
		this.wakeup();
	}

	public void teleport( Vector3f pos ) {
		if (this.destroyed)
			return;

		Transform transform = body.getWorldTransform(new Transform());
		transform.origin.x = pos.x;
		transform.origin.y = pos.y;
		transform.origin.z = pos.z;
		body.setWorldTransform(transform);
	}

	public Matrix4f getWorldMatrix() {
		if (this.destroyed || body == null) {
			if ( luaFrontEnd == null )
				return new Matrix4f();
			
			LuaValue linked = luaFrontEnd.get("Linked");
			if ( linked.isnil() )
				return new Matrix4f();
			
			LuaValue wMat = linked.get("WorldMatrix");
			if ( wMat.isnil() )
				return new Matrix4f();
			
			return ((Matrix4)wMat).toJoml();
		}

		Transform transform = body.getWorldTransform(new Transform());

		float[] fMat = new float[16];
		transform.getOpenGLMatrix(fMat);
		org.joml.Matrix4f worldMatrix = new org.joml.Matrix4f();
		worldMatrix.set(fMat);

		return worldMatrix;
	}

	public Quaternionf getRotation() {
		return getWorldMatrix().getNormalizedRotation(new Quaternionf());
	}

	public void destroy() {
		if ( world != null ) {
			this.world.destroy(this);
		}
		this.destroyed = true;
	}

	public void setMass(float mass) {
		this.desiredShape = null;
		this.refresh = true;
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
		final javax.vecmath.Vector3f vel = this.getVelocityInternal();
		final float newMass = desiredMass;
		RigidBody old = body;
		
		InternalGameThread.runLater(() -> {
			if ( luaFrontEnd.isDestroyed() )
				return;
			
			// Shape
			if ( desiredShape == null ) {
				if ( luaFrontEnd.rawget("UseCustomMesh").checkboolean() && !luaFrontEnd.rawget("CustomMesh").isnil() ) {
					setShapeFromMesh(((Mesh)luaFrontEnd.rawget("CustomMesh")).getMesh());
				} else {
					setShapeFromType(luaFrontEnd.rawget("Shape").toString());
				}
			}
			
			// Destroy old
			if ( old != null && world != null ) {
				world.dynamicsWorld.removeRigidBody(old);
				old.destroy();
			}
			
			// Create new
			RigidBody newBody = PhysicsUtils.getBody(newMass, desiredBounce, desiredFriction, desiredShape);
			newBody.setAngularFactor(desiredAngularFactor);
			newBody.setDamping(desiredLinearDamping, 0.3f);
			
			// Set the body
			PhysicsObjectInternal.this.body = newBody;
			
			// Check for force setting velocity
			if ( forceVelocity != null ) {
				vel.set(forceVelocity);
				forceVelocity = null;
			}
			
			// Set some vars
			setWorldMatrix(worldMat);
			setVelocity(vel);
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
		});
	}

	public void internalTick() {
		if ( this.refresh ) {
			refreshBody();
		}
	}
	
	public void setShapeFromMesh(BufferedMesh mesh) {
		if ( luaFrontEnd == null )
			return;
		
		if ( luaFrontEnd.get("Linked").isnil() || luaFrontEnd.get("Linked").get("Prefab").isnil() ) {
			setShapeFromType("Box");
			return;
		}
		
		float scale = 1.0f;
		LuaValue prefab = luaFrontEnd.get("Linked").get("Prefab");
		if ( !prefab.isnil() ) {
			scale = ((Prefab)prefab).get("Scale").tofloat();
		}
		
		if ( desiredMass == 0 ) {
			desiredShape = PhysicsUtils.meshShapeStatic(mesh, scale );
		} else {
			desiredShape = PhysicsUtils.meshShapeDynamic(mesh, scale);
		}
		
		this.refresh = true;
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
		javax.vecmath.Vector3f halfExtents = new javax.vecmath.Vector3f(width/2f, length/2f, height/2f);
		
		// Fallback shape
		CollisionShape shape = new BoxShape(halfExtents);
		
		// Sphere
		if ( type.equals("Sphere") ) {
			shape = new com.bulletphysics.collision.shapes.SphereShape(minlen/2f);
		}
		
		// Capsule
		if ( type.equals("Capsule") ) {
			shape = new com.bulletphysics.collision.shapes.CapsuleShapeZ(minrad/2f, height-minrad);
		}
		
		// Cylinder
		if ( type.equals("Cylinder") ) {
			shape = new com.bulletphysics.collision.shapes.CylinderShapeZ(halfExtents);
		}
		
		// Hull
		if ( type.equals("Hull") && !luaFrontEnd.get("Linked").get("Prefab").isnil() ) {
			Prefab prefab = (Prefab) luaFrontEnd.get("Linked").get("Prefab");
			BufferedMesh mesh = prefab.getPrefab().getCombinedMesh();
			shape = PhysicsUtils.hullShape(mesh, 1.0f, mesh.getSize() > 64);
		}
		
		this.desiredShape = shape;
		this.refresh = true;
	}

	public boolean isDestroyed() {
		return this.destroyed;
	}

	public void wakeup() {
		if ( this.body == null || this.destroyed )
			return;
		
		this.body.setActivationState(CollisionObject.ACTIVE_TAG);
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
