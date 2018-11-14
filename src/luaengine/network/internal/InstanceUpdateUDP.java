package luaengine.network.internal;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

import engine.Game;
import luaengine.type.object.Instance;
import luaengine.type.object.PhysicsBase;

public class InstanceUpdateUDP implements ClientProcessable {
	public long instanceId;
	public String instanceData;
	
	public InstanceUpdateUDP() {
		this.instanceId = -1;
		this.instanceData = "";
	}
	
	public InstanceUpdateUDP(Instance instance, LuaValue field) {
		this.instanceId = instance.getSID();

		JSONObject j = new JSONObject();
		j.put(field.toString(), PacketUtility.fieldToJSON(instance.get(field)));
		this.instanceData = j.toJSONString();
		
		//System.out.println("Sending update packet -> " + instance + ":" + field + " -> " + instance.get(field));
	}

	@Override
	public void clientProcess() {
		Instance instance = Game.getInstanceFromSID(instanceId);
		//System.out.println("Receiving update packet -> " + instance + "("+instanceId+") / " + instanceData);
		if ( instance == null ) {
			System.out.println("Receiving update packet, but found no instance! " + instanceId);
			return;
		}
		
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
					try {
						instance.set(field, value);
					} catch(Exception e) {
						//
					}
					instance.rawset(field, value);
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
