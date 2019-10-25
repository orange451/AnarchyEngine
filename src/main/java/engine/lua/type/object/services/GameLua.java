package engine.lua.type.object.services;

import java.util.HashMap;
import java.util.UUID;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import engine.Game;
import engine.lua.type.LuaEvent;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;

public class GameLua extends Instance {
	
	public HashMap<Long,Instance> serverSidedInstances = new HashMap<Long,Instance>();
	public HashMap<UUID,Instance> uniqueInstances = new HashMap<>();
	
	public GameLua() {
		super("Game");
		
		// On load event
		this.rawset("Loaded", new LuaEvent());
		this.rawset("Started", new LuaEvent());
		
		// Fields
		this.defineField("Running", LuaValue.valueOf(false), true);
		this.defineField("IsServer", LuaValue.valueOf(false), true);
		
		// GetService convenience method
		getmetatable().set("GetService", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg, LuaValue arg2) {
				Service service = Game.getService(arg2.toString());
				if ( service == null )
					return LuaValue.NIL;
				return service;
			}
		});
		
		this.descendantRemovedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				synchronized(serverSidedInstances) {
					if ( object instanceof Instance ) {
						Instance inst = (Instance) object;
						long sid = inst.getSID();
						
						serverSidedInstances.remove(sid);
					}
				}
				return LuaValue.NIL;
			}
		});
		
		this.descendantAddedEvent().connectLua(new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue object) {
				synchronized(serverSidedInstances) {
					if ( object instanceof Instance ) {
						Instance inst = (Instance)object;
						if ( Game.isServer() ) {
							inst.rawset(C_SID, LuaValue.valueOf(Game.generateSID()));
						}
						
						long sid = inst.getSID();
						
						if ( sid != -1 ) {
							serverSidedInstances.put(sid, inst);
						}
						
						uniqueInstances.put(inst.getUUID(), inst);
					}
				}
				return LuaValue.NIL;
			}
		});
		
		// LOCK HER UP
		setLocked(true);
		setInstanceable(false);
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<Long, Instance> getInstanceMap() {
		return (HashMap<Long, Instance>) serverSidedInstances.clone();
	}
	
	public String getName() {
		return this.get(C_NAME).toString().toLowerCase();
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return null;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
}