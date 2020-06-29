/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;

import engine.Game;
import engine.GameSubscriber;
import engine.InternalRenderThread;
import engine.lua.lib.EnumType;
import engine.lua.network.InternalClient;
import engine.lua.network.InternalServer;
import engine.lua.network.internal.protocol.InstanceUpdateUDP;
import engine.lua.type.LuaConnection;
import engine.lua.type.LuaField;
import engine.lua.type.LuaFieldFlag;
import engine.lua.type.NumberClamp;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Player;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.services.Players;
import engine.physics.PhysicsObjectInternal;
import engine.util.Pair;

public abstract class PhysicsBase extends Instance implements GameSubscriber,Positionable {
	protected GameObject linked;
	protected PhysicsObjectInternal physics;
	private LuaConnection connection;
	private LuaConnection prefabChanged;
	
	/**
	 * If true, the linked instance must have a prefab in order to be physics backed.
	 */
	protected boolean FLAG_REQUIRE_PREFAB = true;
	
	public Player playerOwns;

	protected static final LuaValue C_WORLDMATRIX = LuaValue.valueOf("WorldMatrix");
	protected static final LuaValue C_POSITION = LuaValue.valueOf("Position");
	protected static final LuaValue C_VELOCITY = LuaValue.valueOf("Velocity");
	protected static final LuaValue C_ANGULARVELOCITY = LuaValue.valueOf("AngularVelocity");
	protected static final LuaValue C_PREFAB = LuaValue.valueOf("Prefab");
	protected static final LuaValue C_MASS = LuaValue.valueOf("Mass");
	protected static final LuaValue C_LINKED = LuaValue.valueOf("Linked");
	protected static final LuaValue C_FRICTION = LuaValue.valueOf("Friction");
	protected static final LuaValue C_BOUNCINESS = LuaValue.valueOf("Bounciness");
	protected static final LuaValue C_LINEARDAMPING = LuaValue.valueOf("LinearDamping");
	protected static final LuaValue C_ANGULARFACTOR = LuaValue.valueOf("AngularFactor");
	protected static final LuaValue C_SHAPE = LuaValue.valueOf("Shape");
	protected static final LuaValue C_USECUSTOMMESH = LuaValue.valueOf("UseCustomMesh");
	protected static final LuaValue C_CUSTOMMESH = LuaValue.valueOf("CustomMesh");
	
	private Matrix4f lastWorldMatrix = new Matrix4f();
	
	public PhysicsBase(String typename) {
		super(typename);
		
		this.defineField(C_LINKED, LuaValue.NIL, true);
		
		this.defineField(C_VELOCITY, new Vector3(), false)
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE)
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE_MANUAL);
		
		this.defineField(C_ANGULARVELOCITY, new Vector3(), false)
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE)
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE_MANUAL);
		
		this.defineField(C_WORLDMATRIX, new Matrix4(), false)
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE)
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE_MANUAL);
		
		this.defineField(C_MASS, LuaValue.valueOf(0.5f), false)
							.setClamp(new NumberClamp(0, 1));
		
		this.defineField(C_FRICTION, LuaValue.valueOf(0.1f), false)
							.setClamp(new NumberClampPreferred(0, 10, 0, 1))
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE);
		
		this.defineField(C_BOUNCINESS, LuaValue.valueOf(0.5f), false)
							.setClamp(new NumberClampPreferred(0, 2, 0, 1))
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE);
		
		this.defineField(C_LINEARDAMPING, LuaValue.valueOf(0.0f), false)
							.setClamp(new NumberClamp(0, 1))
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE);
		
		this.defineField(C_ANGULARFACTOR, LuaValue.valueOf(1.0f), false)
							.setClamp(new NumberClamp(0, 1));
		
		this.defineField(C_SHAPE, LuaValue.valueOf("Box"), false)
							.setEnum(new EnumType("Shape"))
							.addFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE);
		
		this.defineField(C_USECUSTOMMESH, LuaValue.valueOf(false), false);
		
		// Apply Force (FORCE, IMPULSE)
		this.getmetatable().set("ApplyForce", new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2, LuaValue arg3) {
				if ( !(arg2 instanceof Vector3) )
					return LuaValue.NIL;
				if ( !(arg3 instanceof Vector3) )
					return LuaValue.NIL;
				if ( physics == null )
					return LuaValue.NIL;
				physics.applyImpulse(((Vector3)arg2).getInternal(), ((Vector3)arg3).getInternal());
				
				return LuaValue.NIL;
			}
		});
		
		Game.getGame().subscribe(this);
		
		// Update matrices
		InternalRenderThread.runLater(()->{
			while( Game.runService() == null )
				try { Thread.sleep(1); } catch(Exception e) { }
			
			Game.runService().renderPreEvent().connect((args)->{
				
				PhysicsObjectInternal tempPhys = this.physics;
				GameObject tempLink = this.linked;
				
				if ( tempPhys != null ) {
					Matrix4f worldMat = lastWorldMatrix;//tempPhys.getWorldMatrix();
					
					if ( tempLink != null ) {
						linked.rawset(C_WORLDMATRIX, new Matrix4(worldMat) );
						//((Matrix4)linked.rawget(C_WORLDMATRIX)).setInternal(worldMat);
					}
					//((Matrix4)this.rawget(C_WORLDMATRIX)).setInternal(worldMat);
					this.rawset(C_WORLDMATRIX, new Matrix4(worldMat) );
				} else if ( tempLink != null ) {
					this.rawset(C_WORLDMATRIX, new Matrix4((Matrix4)linked.get(C_WORLDMATRIX)) );
					//this.rawset(C_WORLDMATRIX, linked.get(C_WORLDMATRIX));
				}
			});
		});
		
		this.changedEvent().connect((args)->{
			if ( args[0].eq_b(C_PARENT) ) {
				playerOwns = null;
				checkNetworkOwnership();
			}
			
			// Client tells server when his player-owned physics changed (Server can still refuse these updates)
			boolean isClient = !Game.isServer();
			if ( isClient && Game.players().getLocalPlayer() != null ) {
				if (Game.players().getLocalPlayer().equals(playerOwns)) {
					LuaField field = this.getField(args[0]);
					if ( field == null )
						return;
					
					if ( !field.hasFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE) )
						return;
					
					if ( field.hasFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE_MANUAL) )
						return;
					
					InternalClient.sendServerUDP(new InstanceUpdateUDP(this, args[0], true));
				}
			}
		});
	}
	
	@Override
	public void onDestroy() {
		cleanupPhysics();
		
		Game.getGame().unsubscribe(this);
	}
	
	public void wakeup() {
		if ( this.physics == null )
			return;
		
		this.physics.wakeup();
	}
	
	@Override
	public void internalTick() {
		PhysicsObjectInternal internalPhys = physics;
		
		if ( linked == null || internalPhys == null || internalPhys.getBody() == null ) {
			checkAddPhysics();
		}
		
		if ( linked == null )
			return;
		if ( internalPhys == null )
			return;
		
		lastWorldMatrix.set(internalPhys.getWorldMatrix());
		
		// Ownership stuff
		checkNetworkOwnership();
		
		// Send update packets over internet!
		if ( internalPhys.getBody() != null && !internalPhys.getBody().isDisposed() ) {
			
			// Server sends physics updates to the clients (except for client-owned physics)
			if ( Game.isServer() && internalPhys.getBody().isActive() && this.getMass() > 0 ) {
				if ( playerOwns == null ) {
					InternalServer.sendAllUDP(new InstanceUpdateUDP(this, C_WORLDMATRIX));
					InternalServer.sendAllUDP(new InstanceUpdateUDP(this, C_VELOCITY));
					InternalServer.sendAllUDP(new InstanceUpdateUDP(this, C_ANGULARVELOCITY));
				} else {
					InternalServer.sendAllUDPExcept(new InstanceUpdateUDP(this, C_WORLDMATRIX), playerOwns.getConnection());
					InternalServer.sendAllUDPExcept(new InstanceUpdateUDP(this, C_VELOCITY), playerOwns.getConnection());
					InternalServer.sendAllUDPExcept(new InstanceUpdateUDP(this, C_ANGULARVELOCITY), playerOwns.getConnection());
				}
			}
			
			// Send our position/velocity to server every tick
			boolean isClient = !Game.isServer();
			if ( isClient && Game.players().getLocalPlayer() != null ) {
				if (Game.players().getLocalPlayer().equals(playerOwns)) {
					InternalClient.sendServerUDP(new InstanceUpdateUDP(this, C_WORLDMATRIX, true));
					InternalClient.sendServerUDP(new InstanceUpdateUDP(this, C_VELOCITY, true));
					InternalClient.sendServerUDP(new InstanceUpdateUDP(this, C_ANGULARVELOCITY, true));
				}
			}
		}
		
		// Update our velocity to the physics objects velocity.
		Vector3f pv = internalPhys.getVelocity();
		this.rawset(C_VELOCITY, new Vector3(pv));
		this.notifyPropertySubscribers(C_VELOCITY, this.rawget(C_VELOCITY));
		
		// Update our angular velocity to the physics objets angular velocity
		Vector3f pav = internalPhys.getAngularVelocity();
		this.rawset(C_ANGULARVELOCITY, new Vector3(pav));
		this.notifyPropertySubscribers(C_ANGULARVELOCITY, this.rawget(C_ANGULARVELOCITY));
	}
	
	/**
	 * Returns the mass of this physics object.
	 * @return
	 */
	public double getMass() {
		return this.get(C_MASS).checkdouble();
	}
	
	private void cleanupPhysics() {
		if ( prefabChanged != null ) {
			prefabChanged.disconnect();
			prefabChanged = null;
		}
		
		if ( connection != null ) {
			connection.disconnect();
			connection = null;
		}
		
		if ( physics != null ) {
			physics.destroy();
			physics = null;
		}
		
		if ( linked != null ) {
			linked = null;
		}
		
		this.rawset(C_LINKED, LuaValue.NIL);
		this.playerOwns = null;
	}
	
	private void setPhysics(LuaValue gameObject) {
		if ( this.destroyed )
			return;
		
		if ( gameObject == null || gameObject.isnil() )
			return;
		
		if ( !(gameObject instanceof GameObject) )
			return;
		
		if ( physics != null ) {
			cleanupPhysics();
		}

		// Linked object must have a prefab
		LuaValue prefab = gameObject.get(C_PREFAB);
		if ( prefab.isnil() && FLAG_REQUIRE_PREFAB )
			return;
		
		// Link us to this game object
		linked = (GameObject) gameObject;
		this.rawset(C_LINKED, linked);
		
		// Cannot continue if we're not linked.
		if ( linked == null )
			return;
		
		
		// Create physics object
		Matrix4 newWorldMatrix = new Matrix4(linked.getWorldMatrix());
		this.rawset(C_WORLDMATRIX, newWorldMatrix);
		physics = new PhysicsObjectInternal(this);
		physics.setVelocity(this.getVelocity().toJoml());
		this.set(C_WORLDMATRIX, newWorldMatrix);
		
		// Initial prefab chnaged event
		if ( linked != null ) {
			setupPrefabChanged();
		}
		
		// Check for changes within parented physics object
		connection = linked.changedEvent().connect((args)->{
			LuaValue property = args[0];
			LuaValue value = args[1];
			
			// Game object had its own matrix changed. Replicate to physics.
			if ( property.eq_b(C_WORLDMATRIX) || property.eq_b(C_POSITION) ) {
				if ( physics == null )
					return;
				
				Matrix4 linkedWorldMat = (Matrix4)value;
				Matrix4 physicsWorldMat = this.getWorldMatrix();
				
				if ( linkedWorldMat.equals(physicsWorldMat) )
					return;
				
				//PhysicsBase.this.set(C_WORLDMATRIX, new Matrix4(((Matrix4)linked.get(C_WORLDMATRIX))));
				PhysicsBase.this.set(C_WORLDMATRIX, (Matrix4)linked.get(C_WORLDMATRIX));
			}
			
			// Game object had its model changed. Replicate to physics object.
			if ( property.equals(C_PREFAB) ) {
				physics.refresh();
				setupPrefabChanged();
			}
		});
		
		checkNetworkOwnership();
	}
	
	protected void forceRefresh() {
		if ( this.physics == null )
			return;
		
		this.physics.refresh();
	}
	
	private void setupPrefabChanged() {
		if ( prefabChanged != null ) {
			prefabChanged.disconnect();
		}
		if ( linked != null ) {
			Prefab prefab = linked.getPrefab();
			if ( prefab != null ) {
				prefabChanged = prefab.changedEvent().connect((args)->{
					physics.refresh();
				});
			}
		}
	}
	
	public void setVelocity(Vector3f velocity) {
		Vector3 vel = new Vector3(velocity);
		this.setVelocity(vel);
	}
	
	public void setVelocity(Vector3 velocity) {
		this.set(C_VELOCITY, velocity);
	}
	
	public float getFriction() {
		return this.get(C_FRICTION).tofloat();
	}
	
	public Vector3 getVelocity() {
		return (Vector3)this.get(C_VELOCITY);
	}

	public Vector3 getAngularVelocity() {
		return (Vector3)this.get(C_ANGULARVELOCITY);
	}
	
	public LuaValue updatePhysics(LuaValue key, LuaValue value) {
		
		// User updated the world matrix
		if ( key.eq_b(C_WORLDMATRIX)) {
			if ( linked != null ) {
				linked.rawset(C_WORLDMATRIX, new Matrix4((Matrix4) value));
			}
			
			if ( physics != null ) {
				physics.setWorldMatrix(((Matrix4)value).toJoml());
			}
			
			return value;
		}
		
		// User updated the velocity
		if ( key.eq_b(C_VELOCITY) ) {
			if ( physics != null ) {
				Vector3 vec = (Vector3)value;
				physics.setVelocity(new com.badlogic.gdx.math.Vector3(vec.getX(), vec.getY(), vec.getZ()));
			}
		}
		
		// Updated Angular Velocity
		if ( key.eq_b(C_ANGULARVELOCITY) ) {
			if ( physics != null ) {
				Vector3 vec = (Vector3)value;
				physics.setAngularVelocity(vec.toJoml());
			}
		}
		
		// User updated the mass
		if ( key.eq_b(C_MASS) ) {
			if ( physics != null ) {
				physics.setMass( Math.min( Math.max( value.tofloat(), 0 ), 1 ) );
			}
		}
		
		// User updated the bounciness
		if ( key.eq_b(C_BOUNCINESS) ) {
			if ( physics != null ) {
				physics.setBounciness(value.tofloat());
			}
		}
		
		// User updated the friction
		if ( key.eq_b(C_FRICTION) ) {
			if ( physics != null ) {
				physics.setFriction(value.tofloat());
			}
		}
		
		// User updated the AngularFactor
		if ( key.eq_b(C_ANGULARFACTOR) ) {
			if ( physics != null ) {
				physics.setAngularFactor( value.tofloat() );
			}
		}
		
		// User updated the LinearDamping
		if ( key.eq_b(C_LINEARDAMPING) ) {
			if ( physics != null ) {
				physics.setLinearDamping(value.tofloat());
			}
		}
		
		// Enable custom mesh
		if ( key.eq_b(C_USECUSTOMMESH) ) {
			if ( value.checkboolean() ) {
				this.defineField(C_CUSTOMMESH.toString(), LuaValue.NIL, false);
				this.set(C_CUSTOMMESH, LuaValue.NIL);
				
				this.getField(C_SHAPE).setLocked(true);
			} else {
				this.undefineField(C_CUSTOMMESH);
				this.getField(C_SHAPE).setLocked(false);
			}

			return value;
		}
		
		// Updated shape
		if ( key.eq_b(C_SHAPE) ) {
			if ( value.isnil() )
				return null;
			
			if (physics != null) {
				physics.setShapeFromType(value.toString());
				physics.refresh();
			}
		}
		
		// If the user wants to set a custom mesh, it MUST be a mesh.
		if ( key.eq_b(C_CUSTOMMESH) ) {
			if ( !value.isnil() && !(value instanceof Mesh) ) {
				return null;
			}
			
			if ( physics != null ) {
				physics.setShapeFromMesh(value);
				physics.refresh();
			}
		}
		return value;
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return updatePhysics(key, value);
	}
	
	@Override
	public void onValueUpdated(LuaValue key, LuaValue value) {
		if ( key.eq_b(C_PARENT) ) {
			//System.out.println("New parent: " + value);
			//cleanupPhysics();
			
			//if ( !value.isnil() && value instanceof Instance && ((Instance)value).isDescendantOf(Game.workspace()) ) {
				//setPhysics(value);
			//}
		}
	}
	
	@Override
	protected boolean onValueGet(LuaValue key) {
		if ( key.eq_b(C_WORLDMATRIX) ) {
			/*if ( physics != null ) {
				((Matrix4)this.rawget("WorldMatrix")).setInternal(physics.getWorldMatrix());
				return true;
			}
			if ( linked != null ) {
				this.rawset("WorldMatrix", linked.get("WorldMatrix"));
				return true;
			}*/
		}
		return true;
	}
	
	private void checkAddPhysics() {
		// Get current parent
		LuaValue par = this.getParent();
		
		// If parent is nil, destroy physics
		if ( par.isnil() ) {
			this.cleanupPhysics();
		} else {
			// If we're parented to something...
			if ( par instanceof Instance ) {
				Instance parent = (Instance) par;
				
				// If the parent is in the workspace...
				if ( parent.isDescendantOf(Game.workspace()) ) {
					if ( this.linked == null || !linked.equals(parent) ) {
						this.cleanupPhysics();
						this.setPhysics(parent);
					}
				} else {
					this.cleanupPhysics();
				}
			} else {
				this.cleanupPhysics();
			}
		}
	}
	
	private void checkNetworkOwnership() {
		if ( playerOwns == null ) {
			Players playerService = Game.players();
			if ( playerService == null )
				return;
			
			if ( Game.isServer() ) {
				
				// This needs to be optimized some way... It's called every step BIG NO NO.
				List<Player> players = playerService.getPlayers();
				for (int i = 0; i < players.size(); i++) {
					Player player = players.get(i);
					Instance character = player.getCharacter();
					if ( character == null )
						continue;
					
					if ( this.isDescendantOf(character) ) {
						playerOwns = player;
					}
				}
			} else {
				LuaValue localPlayer = playerService.getLocalPlayer();
				if ( localPlayer != null && localPlayer instanceof Player ) {
					Player player = (Player) localPlayer;
					Instance character = player.getCharacter();
					if ( character == null )
						return;
					
					//if (player.doesClientOwnPhysics()) {
						if ( this.isDescendantOf(character) ) {
							playerOwns = player;
							System.out.println("I own this");
						}
					//}
				}
			}
		}
	}
	
	public PhysicsObjectInternal getInternal() {
		return this.physics;
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		if ( !important )
			return;
		
		/*if ( !Game.isRunning() ) {
			this.cleanupPhysics();
			return;
		}*/
		
		checkAddPhysics();
	}
	
	/**
	 * Returns the Game Object this physics object is currently linked to.
	 * @return
	 */
	public GameObject getLinked() {
		LuaValue ret = this.get(C_LINKED);
		return ret.isnil()?null:(GameObject)ret;
	}
	
	public Vector3 getPosition() {
		return this.getWorldMatrix().getPosition();
	}

	public void setPosition(Vector3 position) {
		if ( this.linked != null ) {
			linked.setPosition(position);
		} else {
			this.getWorldMatrix().setPosition(position);
		}
	}
	
	public Matrix4 getWorldMatrix() {
		return (Matrix4) this.get(C_WORLDMATRIX);
	}
	
	public void setWorldMatrix(Matrix4 matrix) {
		this.set(C_WORLDMATRIX, matrix);
	}
	
	public abstract Pair<Vector3f, Vector3f> getAABB();
}
