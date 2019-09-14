package engine.lua.network.internal.protocol;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

import com.esotericsoftware.kryonet.Connection;

import engine.Game;
import engine.lua.network.internal.ClientProcessable;
import engine.lua.network.internal.PacketUtility;
import engine.lua.network.internal.ServerProcessable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.insts.PhysicsObject;
import engine.lua.type.object.insts.Player;

public class InstanceUpdateUDP implements ClientProcessable,ServerProcessable {
	public long instanceId;
	public String instanceData;
	public boolean rawOnly;
	
	public InstanceUpdateUDP() {
		this.instanceId = -1;
		this.instanceData = "";
	}
	
	public InstanceUpdateUDP( Instance instance, LuaValue field ) {
		this( instance, field, false );
	}
	
	@SuppressWarnings("unchecked")
	public InstanceUpdateUDP(Instance instance, LuaValue field, boolean rawOnly) {
		this.instanceId = instance.getSID();

		JSONObject j = new JSONObject();
		j.put(field.toString(), PacketUtility.fieldToJSON(instance.get(field)));
		this.instanceData = j.toJSONString();
		
		this.rawOnly = rawOnly;
		
		//System.out.println("Sending update packet -> " + instance + ":" + field + " -> " + instance.get(field));
	}
	
	@Override
	public void serverProcess(Connection connection) {
		Instance instance = Game.getInstanceFromSID(instanceId);
		if ( instance == null ) {
			return;
		}
		
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
		
		// Prevent client from modifying Name, Parent, Classname, or SID.
		try {
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(instanceData);
			String field = (String) obj.keySet().iterator().next();
			
			if ( field.equals(C_NAME) || field.equals(C_PARENT) || field.equals(C_CLASSNAME) || field.equals(C_SID) )
				return;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		// Process it
		process( instance );
	}

	@Override
	public void clientProcess(Connection Connection) {
		Instance instance = Game.getInstanceFromSID(instanceId);
		if ( instance == null )
			return;
		
		process( instance );
	}
	
	private static final String C_NAME = "Name";
	private static final String C_PARENT = "Parent";
	private static final String C_CLASSNAME = "ClassName";
	private static final String C_SID = "SID";
	
	private void process(Instance instance) {
		JSONParser parser = new JSONParser();
		try {
			JSONObject obj = (JSONObject) parser.parse(instanceData);
			String field = (String) obj.keySet().iterator().next();
			LuaValue value = PacketUtility.JSONToField( obj.get(field) );
			
			if ( value != null ) {
				if ( field.equals(C_NAME) )
					instance.forceSetName(value.toString());
				else if ( field.equals(C_PARENT) )
					instance.forceSetParent(value);
				else {
					if ( !rawOnly ) {
						try { instance.set(field, value); } catch(Exception e) {}
					} else {
						if ( Game.isServer() ) {
							if ( instance instanceof PhysicsBase ) {
								((PhysicsBase)instance).updatePhysics(LuaValue.valueOf(field), value);
							}
						} else {
							try { instance.set(field, value); } catch(Exception e) {}
						}
					}
					instance.rawset(field, value);
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
