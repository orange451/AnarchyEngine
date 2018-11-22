package luaengine.type.object;

import java.util.List;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.GameSubscriber;
import engine.physics.PhysicsObjectInternal;
import engine.util.Pair;
import luaengine.network.InternalClient;
import luaengine.network.InternalServer;
import luaengine.network.internal.InstanceUpdateUDP;
import luaengine.type.LuaConnection;
import luaengine.type.data.Matrix4;
import luaengine.type.data.Vector3;
import luaengine.type.object.insts.GameObject;
import luaengine.type.object.insts.Mesh;
import luaengine.type.object.insts.Player;
import luaengine.type.object.insts.Prefab;
import luaengine.type.object.services.Connections;
import luaengine.type.object.services.Players;

public abstract class PhysicsBase extends Instance implements GameSubscriber {
	protected GameObject linked;
	protected PhysicsObjectInternal physics;
	private LuaConnection connection;
	private LuaConnection prefabChanged;
	
	public Player playerOwns;
	
	public PhysicsBase(String typename) {
		super(typename);
		
		this.defineField("Linked", LuaValue.NIL, true);
		this.defineField("Mass", LuaValue.valueOf(0.5f), false);
		this.defineField("Friction", LuaValue.valueOf(0.1f), false);
		this.defineField("Bounciness", LuaValue.valueOf(0.5f), false);
		this.defineField("AngularFactor", LuaValue.valueOf(1.0f), false);
		this.defineField("LinearDamping", LuaValue.valueOf(0.0f), false);
		this.defineField("Shape", LuaValue.valueOf("Box"), false);
		this.defineField("Velocity", Vector3.newInstance(0, 0, 0), false);
		this.defineField("WorldMatrix", new Matrix4(), false);
		this.defineField("UseCustomMesh", LuaValue.valueOf(false), false);
		
		Game.getGame().subscribe(this);
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
		linked.rawset("WorldMatrix", this.get("WorldMatrix"));
		
		// Ownership stuff
		checkNetworkOwnership();
		
		if ( internalPhys.getBody() != null ) {
			if ( Game.isServer() && internalPhys.getBody().isActive() && this.getMass() > 0 ) {
				if ( playerOwns == null ) {
					InternalServer.sendAllUDP(new InstanceUpdateUDP(this, LuaValue.valueOf("WorldMatrix")));
					InternalServer.sendAllUDP(new InstanceUpdateUDP(this, LuaValue.valueOf("Velocity")));
				} else {
					InternalServer.sendAllUDPExcept(new InstanceUpdateUDP(this, LuaValue.valueOf("WorldMatrix")), playerOwns.getConnection());
					InternalServer.sendAllUDPExcept(new InstanceUpdateUDP(this, LuaValue.valueOf("Velocity")), playerOwns.getConnection());					
				}
			}
			
			boolean isClient = !Game.isServer();
			if ( isClient && Game.players().getLocalPlayer() != null ) {
				if (Game.players().getLocalPlayer().equals(playerOwns)) {
					InternalClient.sendServerUDP(new InstanceUpdateUDP(this, LuaValue.valueOf("WorldMatrix"), true));
					InternalClient.sendServerUDP(new InstanceUpdateUDP(this, LuaValue.valueOf("Velocity"), true));
				}
			}
		}
		
		// Update our velocity to the physics objects velocity (if its different).
		Vector3 vel = (Vector3)this.get("Velocity");
		Vector3f tv = vel.toJoml();
		Vector3f pv = internalPhys.getVelocity();
		if ( !tv.equals(pv) ) {
			vel.setInternal(pv);
			this.rawset("Velocity", vel);
			this.notifyPropertySubscribers("Velocity", vel);
		}
	}
	
	/**
	 * Returns the mass of this physics object.
	 * @return
	 */
	public double getMass() {
		return this.get("Mass").checkdouble();
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
		
		this.rawset("Linked", LuaValue.NIL);
		
		this.playerOwns = null;
	}
	
	private void setPhysics(LuaValue value) {
		if ( this.destroyed )
			return;
		
		if ( value == null || value.isnil() )
			return;
		
		if ( !(value instanceof GameObject) )
			return;
		
		if ( physics != null ) {
			cleanupPhysics();
		}

		// Linked object must have a prefab
		LuaValue prefab = value.get("Prefab");
		if ( prefab.isnil() )
			return;
		
		// Link us to this game object
		linked = (GameObject) value;
		this.rawset("Linked", linked);
		
		// Cannot continue if we're not linked.
		if ( linked == null )
			return;
		
		
		// Create physics object
		this.rawset("WorldMatrix", new Matrix4((Matrix4)linked.get("WorldMatrix")));
		physics = new PhysicsObjectInternal(this);
		this.set("WorldMatrix", new Matrix4((Matrix4)linked.get("WorldMatrix")));
		
		// Initial prefab chnaged event
		if ( linked != null ) {
			setupPrefabChanged();
		}
		
		// Check for changes within parented physics object
		connection = linked.changedEvent().connect((args)->{
			LuaValue property = args[0];
			LuaValue svalue = args[1];
			
			// Game object had its own matrix changed. Replicate to physics.
			if ( property.toString().equals("WorldMatrix") || property.toString().equals("Position") ) {
				if ( physics == null )
					return;
				
				PhysicsBase.this.set("WorldMatrix", new Matrix4(((Matrix4)linked.get("WorldMatrix"))));
			}
			
			// Game object had its model changed. Replicate to physics object.
			if ( property.toString().equals("Prefab") ) {
				physics.refresh();
				setupPrefabChanged();
			}
		});
		
		/*InternalGameThread.runLater(()->{
			if ( this.physics != null )
				this.physics.refresh();
		});*/
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
	
	public LuaValue updatePhysics(LuaValue key, LuaValue value) {
		
		// User updated the world matrix
		if ( key.toString().equals("WorldMatrix") ) {
			if ( linked != null ) {
				linked.rawset("WorldMatrix", new Matrix4((Matrix4) value));
			}
			
			if ( physics != null ) {
				physics.setWorldMatrix(((Matrix4)value).toJoml());
			}
			
			return value;
		}
		
		// User updated the velocity
		if ( key.toString().equals("Velocity") ) {
			if ( physics != null ) {
				Vector3 vec = (Vector3)value;
				javax.vecmath.Vector3f newVel = new javax.vecmath.Vector3f( vec.getX(), vec.getY(), vec.getZ() );
				physics.setVelocity(newVel);
			}
		}
		
		// User updated the mass
		if ( key.toString().equals("Mass") ) {
			if ( physics != null ) {
				physics.setMass( Math.min( Math.max( value.tofloat(), 0 ), 1 ) );
			}
		}
		
		// User updated the bounciness
		if ( key.toString().equals("Bounciness") ) {
			if ( physics != null ) {
				physics.setBounciness(Math.min( value.tofloat(), 2));
			}
		}
		
		// User updated the friction
		if ( key.toString().equals("Friction") ) {
			if ( physics != null ) {
				physics.setFriction(Math.min( value.tofloat(), 2));
			}
		}
		
		// User updated the AngularFactor
		if ( key.toString().equals("AngularFactor") ) {
			if ( physics != null ) {
				physics.setAngularFactor( Math.min( value.tofloat(), 1 ) );
			}
		}
		
		// User updated the AngularFactor
		if ( key.toString().equals("LinearDamping") ) {
			if ( physics != null ) {
				physics.setLinearDamping( Math.min( value.tofloat(), 1 ) );
			}
		}
		
		// Enable custom mesh
		if ( key.toString().equals("UseCustomMesh") ) {
			if ( value.checkboolean() ) {
				this.defineField("CustomMesh", LuaValue.NIL, false);
				this.set("CustomMesh", LuaValue.NIL);
			} else {
				this.undefineField("CustomMesh");
			}

			return value;
		}
		
		// Updated shape
		if ( key.toString().equals("Shape") ) {
			if ( value.isnil() )
				return null;
			
			if (physics != null) {
				physics.setShapeFromType(value.toString());
			}
		}
		
		// If the user wants to set a custom mesh, it MUST be a mesh.
		if ( key.toString().equals("CustomMesh") ) {
			if ( !value.isnil() && !(value instanceof Mesh) ) {
				return null;
			}
			
			if ( physics != null ) {
				if ( value.isnil() ) {
					physics.setShapeFromType(this.get("Shape").toString());
				} else {
					physics.setShapeFromMesh(((Mesh)value).getMesh());
				}
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
		if ( key.toString().equals("Parent") ) {
			//System.out.println("New parent: " + value);
			//cleanupPhysics();
			
			//if ( !value.isnil() && value instanceof Instance && ((Instance)value).isDescendantOf(Game.workspace()) ) {
				//setPhysics(value);
			//}
		}
	}
	
	@Override
	protected boolean onValueGet(LuaValue key) {
		if ( key.toString().equals("WorldMatrix") ) {
			if ( physics != null ) {
				((Matrix4)this.rawget("WorldMatrix")).setInternal(physics.getWorldMatrix());
				return true;
			}
			if ( linked != null ) {
				this.rawset("WorldMatrix", linked.get("WorldMatrix"));
				return true;
			}
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
			if ( Game.isServer() ) {
				Connections connections = (Connections) Game.getService("Connections");
				List<GameObject> ownedCharacters = connections.ownedCharacters;
				for (int i = 0; i < ownedCharacters.size(); i++) {
					GameObject character = ownedCharacters.get(i);
					if ( this.isDescendantOf(character) ) {
						Player player = ((Players)Game.getService("Players")).getPlayerFromCharacter(character);
						playerOwns = player;
					}
				}
			} else {
				LuaValue localPlayer = Game.getService("Players").get("LocalPlayer");
				if ( !localPlayer.isnil() && localPlayer instanceof Player ) {
					Player player = (Player) localPlayer;
					
					if (player.get("ClientOwnsPhysics").toboolean()) {
						LuaValue character = player.get("Character");
						if ( !character.isnil() && character instanceof GameObject && this.isDescendantOf((GameObject)character) ) {
							playerOwns = player;
							
							System.out.println("I OWN THIS!");
						}
					}
				}
			}
		}
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		if ( !important )
			return;
		
		if ( !Game.isRunning() ) {
			this.cleanupPhysics();
			return;
		}
		
		checkAddPhysics();
	}

	public abstract Pair<Vector3f, Vector3f> getAABB();
}
