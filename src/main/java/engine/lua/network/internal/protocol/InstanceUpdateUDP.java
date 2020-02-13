/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.network.internal.protocol;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

import com.esotericsoftware.kryonet.Connection;

import engine.Game;
import engine.lua.network.InternalServer;
import engine.lua.network.UUIDSerializable;
import engine.lua.network.internal.ClientProcessable;
import engine.lua.network.internal.JSONUtil;
import engine.lua.network.internal.ServerProcessable;
import engine.lua.type.LuaField;
import engine.lua.type.LuaFieldFlag;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.insts.Player;

public class InstanceUpdateUDP implements ClientProcessable,ServerProcessable {
	public UUIDSerializable instanceUUID;
	public String instanceData;
	public boolean rawOnly;
	
	public InstanceUpdateUDP() {
		this.instanceData = "";
	}
	
	public InstanceUpdateUDP( Instance instance, LuaValue field ) {
		this( instance, field, false );
	}
	
	@SuppressWarnings("unchecked")
	public InstanceUpdateUDP(Instance instance, LuaValue field, boolean rawOnly) {
		this.instanceUUID = new UUIDSerializable(instance.getUUID());

		JSONObject j = new JSONObject();
		j.put(field.toString(), JSONUtil.serializeObject(instance.get(field)));
		this.instanceData = j.toJSONString();
		
		this.rawOnly = rawOnly;
		
		//System.out.println("Sending update packet -> " + instance + ":" + field + " -> " + instance.get(field));
	}
	
	@Override
	public void serverProcess(Connection connection) {
		Instance instance = Game.getInstanceFromUUID(instanceUUID.getUUID());
		if ( instance == null )
			return;
		
		// We only let the client control physics objects FOR NOW.
		if ( !(instance instanceof PhysicsBase) ) {
			return;
		}
		
		// Check if we can process this request
		if ( instance instanceof PhysicsBase ) {
			
			// Get connection object
			engine.lua.type.object.insts.Connection luaConnection = Game.connections().getConnectionFromKryo(connection);
			if ( luaConnection == null ) {
				return;
			}
			
			// Get player from connection
			Player player = luaConnection.getPlayer();
			if ( player == null ) {
				return;
			}
			
			// Get players character
			Instance character = player.getCharacter();
			if ( character == null ) {
				return;
			}
			
			// If physics object belongs to this player...
			if ( !instance.isDescendantOf(character) && !instance.equals(character) ) {
				return;
			}
		}
		
		// Prevent client from modifying Name, Parent, or Classname.
		try {
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(instanceData);
			String field = (String) obj.keySet().iterator().next();
			
			LuaField lField = instance.getField(LuaValue.valueOf(field));
			if ( lField.hasFlag(LuaFieldFlag.CORE_FIELD) )
				return;
			
			if ( !lField.hasFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE) && !lField.hasFlag(LuaFieldFlag.CLIENT_SIDE_REPLICATE_MANUAL) )
				return;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		// Process it
		process( instance, connection );
	}

	@Override
	public void clientProcess(Connection connection) {
		Instance instance = Game.getInstanceFromUUID(instanceUUID.getUUID());
		if ( instance == null )
			return;
		
		process( instance, connection );
	}
	
	private static final String C_NAME = "Name";
	private static final String C_PARENT = "Parent";
	private static final String C_CLASSNAME = "ClassName";
	
	private void process(Instance instance, Connection connection) {
		try {
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(instanceData);
			String field = (String) obj.keySet().iterator().next();
			LuaValue value = JSONUtil.deserializeObject( obj.get(field) );
			
			if ( value != null ) {
				if ( field.equals(C_NAME) )
					instance.forceSetName(value.toString());
				else if ( field.equals(C_PARENT) )
					instance.forceSetParent(value);
				else {

					if ( Game.isServer() ) {
						InternalServer.syncConnectionException = connection;
						try { instance.set(field, value); } catch(Exception e) {}
						InternalServer.syncConnectionException = null;
					} else {
						try { instance.set(field, value); } catch(Exception e) {}
					}
					
					// THIS IS IMPORTANT. This rawset breaks OOP and will not fire changed event...
					if ( rawOnly )
						instance.rawset(field, value);
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
