package luaengine.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback;
import com.bulletphysics.dynamics.RigidBody;

import engine.Game;
import engine.InternalGameThread;
import engine.util.Pair;
import ide.layout.windows.icons.Icons;
import luaengine.type.NumberClamp;
import luaengine.type.object.PhysicsBase;
import luaengine.type.object.TreeViewable;

public class PlayerPhysics extends PhysicsBase implements TreeViewable {

	public PlayerPhysics() {
		super("PlayerPhysics");

		this.defineField("OnGround", LuaValue.valueOf(false), true);
		
		this.defineField("Radius", LuaValue.valueOf(0.3f), false);
		this.getField("Radius").setClamp(new NumberClamp(0.1f, 512));
		
		this.defineField("Height", LuaValue.valueOf(1.0f), false);
		this.getField("Height").setClamp(new NumberClamp(0.1f, 512));
		
		this.defineField("StepHeight", LuaValue.valueOf(0.1f), false);
		this.getField("StepHeight").setClamp(new NumberClamp(0.1f, 512));
		
		// No bounciness
		this.rawset("Bounciness", LuaValue.valueOf(0));
		
		// Force to capsule
		this.set("Shape", "Capsule");
		this.getField("Shape").setLocked(true);
		
		// Use shape
		this.getField("UseCustomMesh").setLocked(true);
		this.rawset("UseCustomMesh", LuaValue.valueOf(false));
		
		// Force it straight up
		this.getField("AngularFactor").setLocked(true);
		this.rawset("AngularFactor", LuaValue.valueOf(0.0f));
		
		// If height changes, rebuild physics
		this.changedEvent().connect((args)-> {
			if ( args[0].toString().equals("Height") ) {
				PlayerPhysics.this.forceRefresh();
			}
		});
	}
	
	@Override
	public void internalTick() {
		super.internalTick();
		
		// Force up
		if ( this.physics != null ) {
			RigidBody body = this.physics.getBody();
			if ( body != null ) {
				body.setSleepingThresholds( 0.0f, 0.0f );
				body.setFriction(0);
			}
			
			float zOff = (this.getHeight()/2f) - this.getStepHeight();
			Vector3f origin = this.getPosition().toJoml().sub(0,0,zOff);
			ClosestRayResultCallback ret = Game.workspace().getPhysicsWorld().rayTestExcluding(origin, new Vector3f(0,0,-this.getStepHeight()*1.1f), this.physics);
			if ( ret.hasHit() ) {
				this.forceset("OnGround", LuaValue.TRUE);
			} else {
				this.forceset("OnGround", LuaValue.FALSE);
			}
			
			if ( this.isOnGround() ) {
				float scale = (float)Math.pow(1f - this.getFriction(), InternalGameThread.delta);
				//System.out.println(scale);
				Vector3f newVel = this.physics.getVelocity().mul(scale, scale, 1);
				this.setVelocity(newVel);
			}
		}
	}
	
	public float getHeight() {
		return this.get("Height").tofloat();
	}
	
	public float getStepHeight() {
		return this.get("StepHeight").tofloat();
	}
	
	public boolean isOnGround() {
		return this.get("OnGround").toboolean();
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_wat;
	}

	@Override
	public Pair<Vector3f, Vector3f> getAABB() {
		float xx = this.rawget("Radius").tofloat();
		float yy = this.rawget("Radius").tofloat();
		float zz = this.rawget("Height").tofloat()/2f;
		
		return new Pair<Vector3f, Vector3f>(new Vector3f(-xx, -yy, -zz), new Vector3f(xx, yy, zz));
	}
}
