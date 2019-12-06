package engine.lua.type.object.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import engine.InternalGameThread;
import engine.lua.type.data.Ray;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.RunScript;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.PhysicsObject;
import engine.lua.type.object.insts.RayResult;
import engine.observer.RenderableWorld;
import engine.observer.Tickable;
import engine.physics.PhysicsObjectInternal;
import engine.physics.PhysicsWorld;
import ide.layout.windows.icons.Icons;

public class Workspace extends Service implements RenderableWorld,TreeViewable,Tickable,RunScript  {
	private static PhysicsWorld physicsWorld;
	private List<Instance> descendants = Collections.synchronizedList(new ArrayList<Instance>());

	private static final LuaValue C_GRAVITY = LuaValue.valueOf("Gravity");
	private static final LuaValue C_CURRENTCAMERA = LuaValue.valueOf("CurrentCamera");

	public Workspace() {
		super("Workspace");

		this.defineField(C_CURRENTCAMERA.toString(), new Camera(), false);
		this.defineField(C_GRAVITY.toString(), LuaValue.valueOf(16), false);
		
		this.getmetatable().set("RayTest", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue ray) {
				try {
					if ( !(ray instanceof Ray) )
						return LuaValue.NIL;
					Ray r = (Ray)ray;
					ClosestRayResultCallback callback = physicsWorld.rayTestClosest(r.getOrigin().getInternal(), r.getDirection().getInternal());
	
					com.badlogic.gdx.math.Vector3 hitWorld;
					callback.getHitPointWorld(hitWorld = new com.badlogic.gdx.math.Vector3());
					com.badlogic.gdx.math.Vector3 hitNormal;
					callback.getHitNormalWorld(hitNormal = new com.badlogic.gdx.math.Vector3());
					
					PhysicsBase p = null;
					btRigidBody body = (btRigidBody) callback.getCollisionObject();
					if ( body != null ) {
						PhysicsObjectInternal phys = physicsWorld.getPhysicsObject(body);
						p = phys.getPhysicsObject();
					}
	
					Vector3 world = new Vector3(hitWorld.x, hitWorld.y, hitWorld.z);
					Vector3 normal = new Vector3(hitNormal.x, hitNormal.y, hitNormal.z);
					
					return new RayResult(p, world, normal);
				} catch(Exception e) {
					e.printStackTrace();
				}
				return LuaValue.NIL;
			}
		});
		
		if ( physicsWorld == null )
			physicsWorld = new PhysicsWorld();
		
		this.descendantRemovedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				synchronized(descendants) {
					descendants.remove((Instance) object);
				}
				return LuaValue.NIL;
			}
		});
		
		this.descendantAddedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				synchronized(descendants) {
					descendants.add((Instance) object);
				}
				return LuaValue.NIL;
			}
		});
		
		InternalGameThread.runLater(()->{
			Camera camera = this.getCurrentCamera();
			if ( camera == null )
				return;
			
			if ( camera.getParent().isnil() )
				camera.forceSetParent(this);
		});
	}
	
	public Camera getCurrentCamera() {
		if ( this.get(C_CURRENTCAMERA).isnil() )
			return null;
		
		return (Camera)this.get(C_CURRENTCAMERA);
	}
	
	public void setCurrentCamera(Camera camera) {
		if ( camera == null || camera.isnil() )
			return;
		
		camera.setParent(this);
		this.set(C_CURRENTCAMERA, camera);
	}

	public void tick() {
		physicsWorld.tick();
		
		synchronized(descendants) {
			for (int i = 0; i < descendants.size(); i++) {
				if ( i >= descendants.size() )
					continue;
				Instance d = descendants.get(i);
				if ( d == null )
					continue;

				d.internalTick();
			}
		}
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.eq_b(C_CURRENTCAMERA) ) {
			if ( value == null || (!value.isnil() && !(value instanceof Camera)) )
				return null;
			
			if ( ((Instance)value).getParent().isnil() )
				((Instance)value).forceSetParent(this);
		}
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		// Destroy physics world
		physicsWorld.destroy();
		physicsWorld = null;
		
		// This no longer has descendants
		descendants.clear();
	}
	
	@Override
	public Icons getIcon() {
		return Icons.icon_workspace;
	}

	public PhysicsWorld getPhysicsWorld() {
		return Workspace.physicsWorld;
	}

	/**
	 * Returns the current gravity in the workspace.
	 * @return
	 */
	public float getGravity() {
		return this.get(C_GRAVITY).tofloat();
	}
	
	/**
	 * Sets the current gravity in the workspace
	 */
	public void setGravity(float gravity) {
		this.set(C_GRAVITY, LuaValue.valueOf(gravity));
	}

	@Override
	public Instance getInstance() {
		return this;
	}
}
