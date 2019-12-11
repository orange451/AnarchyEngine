package engine.lua.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.lib.Enums;
import engine.lua.type.NumberClamp;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.TreeViewable;
import engine.physics.PhysicsObjectInternal;
import engine.physics.PhysicsWorld;
import engine.util.Pair;
import ide.layout.windows.icons.Icons;

public class PlayerPhysics extends PhysicsBase implements TreeViewable {

	private static final LuaValue C_HEIGHT = LuaValue.valueOf("Height");
	private static final LuaValue C_RADIUS = LuaValue.valueOf("Radius");
	private static final LuaValue C_STEPHEIGHT = LuaValue.valueOf("StepHeight");
	private static final LuaValue C_SHAPE = LuaValue.valueOf("Shape");
	private static final LuaValue C_USECUSTOMMESH = LuaValue.valueOf("UseCustomMesh");
	private static final LuaValue C_ANGULARFACTOR = LuaValue.valueOf("AngularFactor");
	private static final LuaValue C_ONGROUND = LuaValue.valueOf("OnGround");
	private static final LuaValue C_BOUNCINESS = LuaValue.valueOf("Bounciness");
	
	public PlayerPhysics() {
		super("PlayerPhysics");
		
		this.FLAG_REQUIRE_PREFAB = false;

		this.defineField(C_ONGROUND.toString(), LuaValue.valueOf(false), true);
		
		this.defineField(C_RADIUS.toString(), LuaValue.valueOf(0.3f), false);
		this.getField(C_RADIUS).setClamp(new NumberClamp(0.1f, 512));
		
		this.defineField(C_HEIGHT.toString(), LuaValue.valueOf(1.0f), false);
		this.getField(C_HEIGHT).setClamp(new NumberClamp(0.1f, 512));
		
		this.defineField(C_STEPHEIGHT.toString(), LuaValue.valueOf(0.1f), false);
		this.getField(C_STEPHEIGHT).setClamp(new NumberClamp(0.1f, 512));
		
		// No bounciness by default
		this.rawset(C_BOUNCINESS, LuaValue.valueOf(0));
		
		// Force to capsule
		this.set(C_SHAPE.toString(), Enums.matchEnum(C_SHAPE, LuaValue.valueOf("Capsule")));
		this.getField(C_SHAPE).setLocked(true);
		
		// Use shape
		this.getField(C_USECUSTOMMESH.tostring()).setLocked(true);
		this.rawset(C_USECUSTOMMESH, LuaValue.valueOf(false));
		
		// Force it straight up
		this.getField(C_ANGULARFACTOR).setLocked(true);
		this.rawset(C_ANGULARFACTOR, LuaValue.valueOf(0.0f));
		
		// If height changes, rebuild physics
		this.changedEvent().connect((args)-> {
			if ( args[0].eq_b(C_HEIGHT) ) {
				PlayerPhysics.this.forceRefresh();
			}
		});
	}
	
	@Override
	public void internalTick() {
		super.internalTick();
		
		// Force up
		if ( this.physics != null ) {
			btRigidBody body = this.physics.getBody();
			if ( body != null ) {
				body.setSleepingThresholds( 0.0f, 0.0f );
				body.setFriction(0);
				body.setAngularFactor(0);
				body.setAngularVelocity( Vector3.Zero );
			}
			
			/*float zOff = (this.getHeight()/2f) - this.getStepHeight();
			Vector3f origin = this.getPosition().toJoml().sub(0,0,zOff);
			ClosestRayResultCallback ret = Game.workspace().getPhysicsWorld().rayTestExcluding(origin, new Vector3f(0,0,-this.getStepHeight()*1.1f), this.physics);
			if ( ret.hasHit() ) {
				this.forceset(C_ONGROUND, LuaValue.TRUE);
			} else {
				this.forceset(C_ONGROUND, LuaValue.FALSE);
			}
			ret.dispose();*/
			doFloorCollisionNew();
			
			if ( this.isOnGround() ) {
				float scale = (float)Math.pow(1f - this.getFriction(), InternalGameThread.delta);
				Vector3f newVel = this.physics.getVelocity().mul(scale, scale, 1);
				this.setVelocity(newVel);
			}
		}
	}
	
	private void doFloorCollisionNew() {
		float extraStepHeight = 0;
		float stepHeight = this.getStepHeight();
		if ( !this.isOnGround() )
			stepHeight /= 4f;
		
		if ( this.getVelocity().getZ() > -0.3 && this.getVelocity().getZ() <= 0 )
			extraStepHeight = stepHeight;
		
		PhysicsWorld physics = Game.workspace().getPhysicsWorld();
	
		// 64 rays cast
		int steps = 16;
		int rings = 4;
	
		// Calculate distance between rings
		float margin = 0.01f;
		float radius = this.getRadius();
		float march = (float) ((radius - margin) / rings); 
	
		// get starting location
		Vector3f origin = this.physics.getLocation().sub(0,0,this.getHeight()/2f);
		float x = origin.x;
		float y = origin.y;
		float z = origin.z + stepHeight - margin;
	
		// Cast all rays
		ClosestRayResultCallback[] callbacks = new ClosestRayResultCallback[steps * rings +1];
		callbacks[callbacks.length - 1] = physics.rayTestClosest( new Vector3f( x, y, z ), new Vector3f( 0, 0, -(stepHeight+extraStepHeight) ));
		for (int ii = 0; ii < rings; ii++) {
			for (int i = 0; i < steps; i++) {
				float frac = (float) ((i/(float)steps) * Math.PI * 2);
				float size = (radius - margin) - (march * ii);
				float xx = (float) (x + (Math.cos(frac + ii) * size));
				float yy = (float) (y + (Math.sin(frac + ii) * size));
	
				callbacks[(ii * steps) + i] = physics.rayTestClosest( new Vector3f( xx, yy, z ), new Vector3f( 0, 0, -(stepHeight+extraStepHeight) ));
			}
		}
		
		// Loop through all rays and find one closest to the ground
		float dist = stepHeight+extraStepHeight;
		ClosestRayResultCallback closest = null;
		for (int i = 0; i < callbacks.length; i++) {
			ClosestRayResultCallback callback = callbacks[i];
			if ( callback.getCollisionObject() == null )
				continue;
			
			Vector3 hitPoint = new Vector3();
			callback.getHitPointWorld(hitPoint);
	
			// find lowest ray
			float d = z - hitPoint.z;
			if (d < dist) {
				// Perform another ray test to see if there is any geometry right above the potential floor.
				ClosestRayResultCallback temp = physics.rayTestClosest( new Vector3f( hitPoint.x, hitPoint.y, hitPoint.z + (radius * 1.1f) ), new Vector3f( 0, 0, getHeight()-radius ));
				Vector3 tempHitPoint = new Vector3();
				temp.getHitPointWorld(tempHitPoint);
				float dz = tempHitPoint.z - hitPoint.z;
	
				// If there is nothing blocking the ray, then it is a potential position we can move into!
				//if ( temp.getCollisionObject() == null || dz >= getHeight()-radius ) {
					closest = callback;
					dist = d;
				//}
			}
		}
		
		boolean onGround = false;
		
		// We have a new floor!
		if ( closest != null && closest.getCollisionObject() != null ) {
			// Get z position of floor
			Vector3 hitLowest = new Vector3();
			closest.getHitPointWorld(hitLowest);
			float toz = hitLowest.z;
			
			// Teleport z to floor
			Vector3f toPos = new Vector3f(x, y, toz + getHeight()/2f + (margin/2f));
			this.physics.teleport( toPos );
			
			// Get rid of z velocity (on floor)
			Vector3f currentVel = this.physics.getVelocity();
			currentVel.z = 0;
			this.physics.setVelocity(currentVel);
			
			// Mark that we're on ground
			onGround = true;
		}
		
		this.forceset(C_ONGROUND, LuaValue.valueOf(onGround));
	}
	
	private void doFloorCollision() {
		float stepHeight = this.getStepHeight();
		if ( !this.isOnGround() )
			stepHeight /= 4f;
		PhysicsWorld physics = Game.workspace().getPhysicsWorld();
	
		// 64 rays cast
		int steps = 16;
		int rings = 4;
	
		// Calculate distance between rings
		float margin = 0.01f;
		float radius = this.getRadius();
		float march = (float) ((radius - margin) / rings);
	
		// get starting location
		Vector3f origin = this.physics.getLocation().sub(0,0,this.getHeight()/2f);
		float x = origin.x;
		float y = origin.y;
		float z = origin.z + stepHeight;
	
		// Cast all rays
		ClosestRayResultCallback[] callbacks = new ClosestRayResultCallback[steps * rings +1];
		callbacks[callbacks.length - 1] = physics.rayTestClosest( new Vector3f( x, y, z ), new Vector3f( 0, 0, -stepHeight ));
		for (int ii = 0; ii < rings; ii++) {
			for (int i = 0; i < steps; i++) {
				float frac = (float) ((i/(float)steps) * Math.PI * 2);
				float size = (radius - margin) - (march * ii);
				float xx = (float) (x + (Math.cos(frac + ii) * size));
				float yy = (float) (y + (Math.sin(frac + ii) * size));
	
				callbacks[(ii * steps) + i] = physics.rayTestClosest( new Vector3f( xx, yy, z ), new Vector3f( 0, 0, -stepHeight ));
			}
		}
	
		Vector3f slope = new Vector3f(0, 0, 1);
		int amtSteep = 0;
		int amtFlat = 0;
		float steepAngle = 0.82f;
	
		// Loop through all rays and find one closest to the ground
		float dist = 999;
		ClosestRayResultCallback lowest = null;
		for (int i = 0; i < callbacks.length; i++) {
			ClosestRayResultCallback callback = callbacks[i];
	
			// Ignore the ray if it's above you (bug)
			/*if ( callback.getHitPointWorld(new Vector3());.z > z )
				continue;*/
			
			Vector3 hitPoint = new Vector3();
			callback.getHitPointWorld(hitPoint);
	
			// find lowest ray
			float d = z - hitPoint.z;
			if (d < dist) {
				// Perform another ray test to see if there is any geometry right above the potential floor.
				ClosestRayResultCallback temp = physics.rayTestClosest( new Vector3f( hitPoint.x, hitPoint.y, hitPoint.z + 0.5f ), new Vector3f( 0, 0, 1 ));
				Vector3 tempHitPoint = new Vector3();
				temp.getHitPointWorld(tempHitPoint);
				float dz = tempHitPoint.z - hitPoint.z;
	
				// If there is nothing blocking the ray, then it is a potential position we can move into!
				if ( tempHitPoint.z == 0 || dz >= getHeight() ) {
					lowest = callback;
					dist = d;
				}
			}
	
			// Find slope
			/*if (d < stepHeight) {
				Vector3 hitNormal = new Vector3();
				callback.getHitNormalWorld(hitNormal);
				
				//hitNormal.normalize();
				if (Math.abs(hitNormal.z) <= steepAngle) {
					slope.add(hitNormal.x, hitNormal.y, hitNormal.z);
					amtSteep++;
				}else{
					amtFlat++;
				}
			}*/
		}
	
		// Put distace at very bottom of collision shape.
		dist -= stepHeight;
	
		// Get current velocity;
		Vector3f newVel = this.getVelocity().toJoml();
	
		// Don't look at floor collisions if velocity is going up
		if (newVel.z > 4.0)
			return;
	
		// Reset motion states
		boolean wasOnGroundBefore = this.isOnGround();
		PhysicsObjectInternal objectHit = null;
		Vector3f hitVelocity = this.getVelocity().toJoml();
		boolean onGround = false;
		boolean sliding = false;
		if ( lowest != null && dist < stepHeight ) {
			onGround = true;
			Vector3 lowestHitPoint = new Vector3();
			lowest.getHitPointWorld(lowestHitPoint);
			
			float toz = lowestHitPoint.z;
			float zDelta = toz - origin.z;
	
			// If we're moving upwards
			if ( zDelta > 0.6 ) {
				// Do not climb up objects that are too steep OR are a moving rigid body
				if ( Math.abs( slope.z ) < steepAngle || Math.abs( slope.z ) > 1.0 ) {
					toz = origin.z;
				}
			}
	
			// Calculate new position to stand on
			Vector3f toPos = new Vector3f(x, y, toz + getHeight()/2f);
			if ( lowest.getCollisionObject() != null ) {
				objectHit = physics.getPhysicsObject((btRigidBody) lowest.getCollisionObject());
			}
	
			// Move the player along with moving objects that he's standing on
			/*if (lowest.getCollisionObject() != null) {
				Vector3f vel = lowest.collisionObject.getInterpolationLinearVelocity(new Vector3f());
				toPos.x += vel.x * GameEngine.delta;
				toPos.y += vel.y * GameEngine.delta;
				toPos.z += vel.z * GameEngine.delta;
				objectHit = physics.findObject( (RigidBody) lowest.collisionObject );
				if (objectHit != null) {
					
					// Set the material we're on to the material directly under us
					materialOn = objectHit.getSoundMaterial();
					
					// hacky stuff dont look
					Vector3f start = lowest.rayFromWorld;
					Vector3f start1 = new Vector3f( start.x, start.y, start.z );
					ClosestRayResultCallback callmebackpls = physics.rayTestClosestWorld(start1, new Vector3f( 0, 0, -1 ), false );
					if ( callmebackpls.collisionObject != null ) {
						PhysicsObject tempObj = physics.findObject( (RigidBody) callmebackpls.collisionObject );
						if ( tempObj != null) {
							materialOn = tempObj.getSoundMaterial();
						}
					}
				}
			}*/
	
			// Teleport player to the correct location
			this.physics.teleport( toPos );
	
			// Check for a hill
			if (amtSteep > amtFlat) {
				slope.normalize();
				newVel.add(slope.mul( newVel.dot(slope) * -1f ));
				//newVel = VectorMath.add(VectorMath.mult(slope, VectorMath.dot(newVel, slope) * -1f), newVel);
				sliding = true;
				onGround = false;
			} else {
				newVel.z = 0;
			}
	
			// Set the z velocity. 0 if on ground.
			this.physics.setVelocity(newVel);
		}
		
		// If we hit the ground
		if ( !wasOnGroundBefore && onGround && objectHit != null && objectHit.getBody().getInvMass() > 0 ) {
			Vector3f nv = new Vector3f( hitVelocity.x / 2f, hitVelocity.y / 2f, hitVelocity.z / 2f );
			Vector3f v = objectHit.getVelocity().add(nv);
			objectHit.setVelocity( v );
		}
		
		this.forceset(C_ONGROUND, LuaValue.valueOf(onGround));
	}
	
	public float getRadius() {
		return this.get(C_RADIUS).tofloat();
	}
	
	public float getHeight() {
		return this.get(C_HEIGHT).tofloat();
	}
	
	public float getStepHeight() {
		return this.get(C_STEPHEIGHT).tofloat();
	}
	
	public boolean isOnGround() {
		return this.get(C_ONGROUND).toboolean();
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_wat;
	}

	@Override
	public Pair<Vector3f, Vector3f> getAABB() {
		float xx = this.rawget(C_RADIUS).tofloat();
		float yy = this.rawget(C_RADIUS).tofloat();
		float zz = this.rawget(C_HEIGHT).tofloat()/2f;
		
		return new Pair<Vector3f, Vector3f>(new Vector3f(-xx, -yy, -zz), new Vector3f(xx, yy, zz));
	}
}
