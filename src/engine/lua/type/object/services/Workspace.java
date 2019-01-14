package engine.lua.type.object.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import engine.lua.LuaEngine;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.Instance;
import engine.lua.type.object.RunScript;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Camera;
import engine.observer.Tickable;
import engine.physics.PhysicsWorld;
import ide.layout.windows.icons.Icons;

public class Workspace extends Service implements TreeViewable,Tickable,RunScript  {
	private static PhysicsWorld physicsWorld;
	private List<Instance> descendants = Collections.synchronizedList(new ArrayList<Instance>());

	public Workspace() {
		super("Workspace");
		LuaEngine.globals.set("workspace", this);

		this.defineField("CurrentCamera", new Camera(), false);
		this.defineField("Gravity", LuaValue.valueOf(16), false);
		
		if ( physicsWorld == null )
			physicsWorld = new PhysicsWorld();
		
		((LuaEvent)this.rawget("DescendantRemoved")).connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				synchronized(descendants) {
					descendants.remove((Instance) object);
				}
				return LuaValue.NIL;
			}
		});
		((LuaEvent)this.rawget("DescendantAdded")).connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				synchronized(descendants) {
					descendants.add((Instance) object);
				}
				return LuaValue.NIL;
			}
		});
	}
	
	public Camera getCurrentCamera() {
		if ( this.get("CurrentCamera").isnil() )
			return null;
		
		return (Camera)this.get("CurrentCamera");
	}
	
	public void setCurrentCamera(Camera camera) {
		if ( camera == null || camera.isnil() )
			return;
		
		this.set("CurrentCamera", camera);
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
		if ( key.toString().equals("CurrentCamera") ) {
			if ( value == null || (!value.isnil() && !(value instanceof Camera)) )
				return null;
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
		return this.physicsWorld;
	}
}
