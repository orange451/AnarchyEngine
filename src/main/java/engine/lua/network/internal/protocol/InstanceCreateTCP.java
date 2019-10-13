package engine.lua.network.internal.protocol;

import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.luaj.vm2.LuaValue;

import com.esotericsoftware.kryonet.Connection;

import engine.lua.network.internal.ClientProcessable;
import engine.lua.network.internal.NonReplicatable;
import engine.lua.network.internal.JSONUtil;
import engine.lua.type.object.Instance;

public class InstanceCreateTCP implements ClientProcessable {
	public String instanceType;
	public String instanceData;

	private static final String C_PARENTs = "Parent";
	private static final LuaValue C_CLASSNAME = LuaValue.valueOf("ClassName");
	private static final LuaValue C_NAME = LuaValue.valueOf("Name");
	private static final LuaValue C_PARENT = LuaValue.valueOf(C_PARENTs);
	private static final LuaValue C_SID = LuaValue.valueOf("SID");
	
	public InstanceCreateTCP() {
		this.instanceType = "";
		this.instanceData = "";
	}
	
	@SuppressWarnings("unchecked")
	public InstanceCreateTCP(Instance instance) {
		this.instanceType = instance.get(C_CLASSNAME).toString();
		
		LuaValue[] fields = instance.getFields();
		
		JSONObject j = new JSONObject();
		for (int i = 0; i < fields.length; i++) {
			LuaValue field = fields[i];
			
			if ( field.eq_b(C_CLASSNAME) )
				continue;
			
			if ( instance instanceof NonReplicatable ) {
				if ( !field.eq_b(C_NAME) && !field.eq_b(C_PARENT) && !field.eq_b(C_SID) ) {
					continue;
				}
			}
			
			j.put(field, JSONUtil.serializeField(instance.get(field)));
		}
		
		this.instanceData = j.toJSONString();
	}

	@Override
	public void clientProcess(Connection Connection) {
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
				LuaValue value = JSONUtil.deserializeField(jsonValue);
				//System.out.println(internalInstance + " :: " + field + " / " + value + " / " + jsonValue);
				if ( value != null ) {
					if ( field.equals(C_PARENTs) ) {
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
