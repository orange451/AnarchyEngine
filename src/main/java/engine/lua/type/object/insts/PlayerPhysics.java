package engine.lua.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.type.NumberClamp;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.TreeViewable;
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
		this.set(C_SHAPE.toString(), "Capsule");
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
			
			float zOff = (this.getHeight()/2f) - this.getStepHeight();
			Vector3f origin = this.getPosition().toJoml().sub(0,0,zOff);
			ClosestRayResultCallback ret = Game.workspace().getPhysicsWorld().rayTestExcluding(origin, new Vector3f(0,0,-this.getStepHeight()*1.1f), this.physics);
			if ( ret.hasHit() ) {
				this.forceset(C_ONGROUND, LuaValue.TRUE);
			} else {
				this.forceset(C_ONGROUND, LuaValue.FALSE);
			}
			
			if ( this.isOnGround() ) {
				float scale = (float)Math.pow(1f - this.getFriction(), InternalGameThread.delta);
				Vector3f newVel = this.physics.getVelocity().mul(scale, scale, 1);
				this.setVelocity(newVel);
			}
		}
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
