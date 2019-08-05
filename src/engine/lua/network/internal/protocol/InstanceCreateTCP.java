package engine.lua.network.internal.protocol;

import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

import engine.lua.network.internal.ClientProcessable;
import engine.lua.network.internal.PacketUtility;
import engine.lua.type.object.Instance;

public class InstanceCreateTCP implements ClientProcessable {
	public String instanceType;
	public String instanceData;
	
	public InstanceCreateTCP() {
		this.instanceType = "";
		this.instanceData = "";
	}
	
	@SuppressWarnings("unchecked")
	public InstanceCreateTCP(Instance instance) {
		this.instanceType = instance.get("ClassName").toString();
		
		String[] fields = instance.getFields();
		
		JSONObject j = new JSONObject();
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i];
			
			if ( field.equals("ClassName") )
				continue;
			
			j.put(field, PacketUtility.fieldToJSON(instance.get(field)));
		}
		
		this.instanceData = j.toJSONString();
	}

	@Override
	public void clientProcess() {
		LuaValue toParent = LuaValue.NIL;
		//System.out.println("Attempting to create: " + instanceType);
		Instance internalInstance = (Instance) Instance.instance(instanceType);
		if ( internalInstance == null )
			return;
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject obj = (JSONObject) parser.parse(instanceData);
			
			Set<?> keys = obj.keySet();
			Object[] keyArray = keys.toArray();
			for (int i = 0; i < keyArray.length; i++) {
				String field = (String) keyArray[i];
				Object jsonValue = obj.get(field);
				LuaValue value = PacketUtility.JSONToField(jsonValue);
				//System.out.println(internalInstance + " :: " + field + " / " + value + " / " + jsonValue);
				if ( value != null ) {
					if ( field.equals("Parent") ) {
						toParent = value;
					} else {
						internalInstance.rawset(field, value);
						try{internalInstance.set(field, value);}catch(Exception e) {}
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		//System.out.println("PARENT: " + toParent);
		internalInstance.forceSetParent(toParent);
	}
}
