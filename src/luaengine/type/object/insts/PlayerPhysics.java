package luaengine.type.object.insts;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import com.bulletphysics.dynamics.RigidBody;

import engine.util.Pair;
import ide.layout.windows.icons.Icons;
import luaengine.type.object.PhysicsBase;
import luaengine.type.object.TreeViewable;

public class PlayerPhysics extends PhysicsBase implements TreeViewable {

	public PlayerPhysics() {
		super("PlayerPhysics");
		
		this.defineField("Radius", LuaValue.valueOf(0.3f), false);
		this.defineField("Height", LuaValue.valueOf(1.0f), false);
		this.defineField("StepHeight", LuaValue.valueOf(0.1f), false);
		
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
			PlayerPhysics.this.forceRefresh();
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
			}
		}
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
