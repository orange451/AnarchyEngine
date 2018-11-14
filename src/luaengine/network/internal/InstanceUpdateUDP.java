package luaengine.network.internal;

import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

import engine.Game;
import luaengine.type.object.Instance;
import luaengine.type.object.PhysicsBase;
import luaengine.type.object.insts.GameObject;
import luaengine.type.object.insts.PhysicsObject;
import luaengine.type.object.services.Connections;

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
	
	public InstanceUpdateUDP(Instance instance, LuaValue field, boolean rawOnly) {
		this.instanceId = instance.getSID();

		JSONObject j = new JSONObject();
		j.put(field.toString(), PacketUtility.fieldToJSON(instance.get(field)));
		this.instanceData = j.toJSONString();
		
		this.rawOnly = rawOnly;
		
		//System.out.println("Sending update packet -> " + instance + ":" + field + " -> " + instance.get(field));
	}
	
	@Override
	public void serverProcess() {
		Instance instance = Game.getInstanceFromSID(instanceId);
		if ( instance == null )
			return;
		
		// We only let the client control physics objects FOR NOW.
		if ( !(instance instanceof PhysicsObject) )
			return;
		
		// Check if we can process this request
		boolean can = false;
		Connections con = (Connections) Game.getService("Connections");
		List<GameObject> characters = con.ownedCharacters;
		for (int i = 0; i < characters.size(); i++) {
			GameObject character = characters.get(i);
			if ( instance.isDescendantOf(character) || instance.equals(character) )
				can = true;
		}
		
		if ( !can )
			return;
		
		// Get the field being modified
		try {
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(instanceData);
			String field = (String) obj.keySet().iterator().next();
			
			if ( field.equals("Name") || field.equals("Parent") || field.equals("ClassName") || field.equals("SID") )
				return;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		// Process it
		process( instance );
	}

	@Override
	public void clientProcess() {
		Instance instance = Game.getInstanceFromSID(instanceId);
		//System.out.println("Receiving update packet -> " + instance + "("+instanceId+") / " + instanceData);
		if ( instance == null )
			return;
		
		process( instance );
	}
	
	private void process(Instance instance) {
		JSONParser parser = new JSONParser();
		try {
			JSONObject obj = (JSONObject) parser.parse(instanceData);
			String field = (String) obj.keySet().iterator().next();
			LuaValue value = PacketUtility.JSONToField( obj.get(field) );
			
			if ( value != null ) {
				if ( field.equals("Name") )
					instance.forceSetName(value.toString());
				else if ( field.equals("Parent") )
					instance.forceSetParent(value);
				else {
					if ( !rawOnly ) {
						try {
							instance.set(field, value);
						} catch(Exception e) {
							//
						}
					} else {
						if ( instance instanceof PhysicsObject ) {
							((PhysicsObject)instance).updatePhysics(LuaValue.valueOf(field), value);
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
