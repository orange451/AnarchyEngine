/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.lib.Enums;
import engine.lua.lib.FourArgFunction;
import engine.lua.lib.LuaUtil;
import engine.lua.type.data.Ray;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.ScriptExecutor;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.RayResult;
import engine.observer.RenderableWorld;
import engine.observer.Tickable;
import engine.physics.PhysicsObjectInternal;
import engine.physics.PhysicsWorld;
import ide.layout.windows.icons.Icons;

public class Workspace extends Service implements RenderableWorld,TreeViewable,Tickable,ScriptExecutor  {
	private static PhysicsWorld physicsWorld;
	private List<Instance> descendants = Collections.synchronizedList(new ArrayList<Instance>());

	private static final LuaValue C_GRAVITY = LuaValue.valueOf("Gravity");
	private static final LuaValue C_CURRENTCAMERA = LuaValue.valueOf("CurrentCamera");
	private static final LuaValue C_RAYIGNORETYPE = LuaValue.valueOf("RayIgnoreType");

	public Workspace() {
		super("Workspace");

		this.defineField(C_CURRENTCAMERA.toString(), new Camera(), false);
		this.defineField(C_GRAVITY.toString(), LuaValue.valueOf(16), false);
		
		// RayTest( Ray, [ExclusionList], [RayIgnoreType])
		this.getmetatable().set("RayTest", new FourArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue ray, LuaValue exclusionList, LuaValue rayTypeEnum) {
				try {
					if ( !(ray instanceof Ray) )
						return LuaValue.NIL;
					
					if ( exclusionList.isnil() )
						exclusionList = new LuaTable();
					
					if ( !(exclusionList instanceof LuaTable) )
						return LuaValue.NIL;
					
					LuaValue e = Enums.matchEnum(C_RAYIGNORETYPE, rayTypeEnum);
					if ( e == null && !rayTypeEnum.isnil() )
						return LuaValue.NIL;
					
					if ( !exclusionList.isnil() && !rayTypeEnum.isnil() ) {
						// Get all root instances in the selection
						LuaTable table = (LuaTable)exclusionList;
						List<Instance> instances = LuaUtil.tableToList(table);
						List<Instance> rootObjects = Game.getRootInstances(instances);
						
						// Get all PHYSICS BASE descendents of the root instances (inclusive)
						List<Instance> descendents = new ArrayList<Instance>();
						for (int i = 0; i < rootObjects.size(); i++) {
							Instance root = rootObjects.get(i);
							if ( root instanceof PhysicsBase )
								descendents.add(root);
							
							List<Instance> desc = root.getDescendantsUnsafe();
							for (int j = 0; j < desc.size(); j++) {
								Instance t = desc.get(j);
								if ( t instanceof PhysicsBase ) {
									descendents.add(t);
								}
							}
						}
						
						// Get array of internal physics objects from previous list
						PhysicsObjectInternal[] excluding = new PhysicsObjectInternal[descendents.size()];
						for (int i = 0; i < excluding.length; i++) {
							excluding[i] = ((PhysicsBase)descendents.get(i)).getInternal();
						}
						
						// Perform ray exclusion test
						Ray r = (Ray)ray;
						ClosestRayResultCallback callback = physicsWorld.rayTestExcluding(r.getOrigin().getInternal(), r.getDirection().getInternal(), excluding);
						return getRayResult(callback);
					} else {
						
						// Perform standard ray test
						Ray r = (Ray)ray;
						ClosestRayResultCallback callback = physicsWorld.rayTestClosest(r.getOrigin().getInternal(), r.getDirection().getInternal());
						return getRayResult(callback);
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				return LuaValue.NIL;
			}
		});
		
		// Setup physics world
		if ( physicsWorld == null )
			physicsWorld = new PhysicsWorld();
		
		// Remove from CUSTOM descendAnts array
		this.descendantRemovedEvent().connect((args)->{
			synchronized(descendants) {
				descendants.remove((Instance) args[0]);
			}
		});
		
		// Add to CUSTOM descendAnts array
		this.descendantAddedEvent().connect((args)->{
			synchronized(descendants) {
				descendants.add((Instance) args[0]);
			}
		});
		
		// Make sure camera is inside workspace :wink:
		InternalGameThread.runLater(()->{
			Camera camera = this.getCurrentCamera();
			if ( camera == null )
				return;
			
			if ( camera.getParent().isnil() )
				camera.forceSetParent(this);
		});
	}
	
	protected RayResult getRayResult(ClosestRayResultCallback callback) {
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
	}

	public Camera getCurrentCamera() {
		if ( this.get(C_CURRENTCAMERA).isnil() )
			return null;
		
		return (Camera)this.get(C_CURRENTCAMERA);
	}
	
	public void setCurrentCamera(Camera camera) {
		if ( camera == null || camera.isnil() || camera.isDestroyed() )
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
