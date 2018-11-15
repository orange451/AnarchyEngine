package luaengine.network.internal;

import java.lang.reflect.Method;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaValue;

import engine.Game;
import luaengine.type.LuaValuetype;
import luaengine.type.object.Instance;

public class PacketUtility {
	protected static Object fieldToJSON(LuaValue luaValue) {
		if ( luaValue.isstring() )
			return luaValue.toString();
		if ( luaValue.isboolean() )
			return luaValue.checkboolean();
		if ( luaValue.isnumber() )
			return luaValue.todouble();

		// Instances in the game
		if ( luaValue instanceof Instance || luaValue.isnil() ) {
			long refId = luaValue.isnil()?-1:((Instance)luaValue).getSID();			
			JSONObject j = new JSONObject();
			j.put("Type", "Reference");
			j.put("Value", refId);
			return j;
		}

		// Vectorx/Colors/etc
		if ( luaValue instanceof LuaValuetype ) {
			JSONObject t = new JSONObject();
			t.put("ClassName", ((LuaValuetype)luaValue).typename());
			t.put("Data", ((LuaValuetype)luaValue).toJSON());

			JSONObject j = new JSONObject();
			j.put("Type", "Datatype");
			j.put("Value", t);
			return j;
		}

		return null;
	}
	
	protected static LuaValue JSONToField(Object t) {
		if ( t == null ) {
			return LuaValue.NIL;
		}
		if ( t instanceof Boolean ) {
			return LuaValue.valueOf((Boolean)t);
		}
		if ( t instanceof Double ) {
			return LuaValue.valueOf((Double)t);
		}
		if ( t instanceof Float ) {
			return LuaValue.valueOf((Float)t);
		}
		if ( t instanceof String ) {
			return LuaValue.valueOf((String)t);
		}
		if ( t instanceof JSONObject ) {
			JSONObject j = (JSONObject)t;
			
			if ( j.get("Type").equals("Reference") ) {
				long v = Long.parseLong(""+j.get("Value"));
				return Game.getInstanceFromSID(v);
			}
			
			if ( j.get("Type").equals("Datatype") ) {
				JSONObject data = (JSONObject) j.get("Value");
				String type = (String) data.get("ClassName");
				JSONObject temp = (JSONObject) data.get("Data");
				
				Class<? extends LuaValuetype> c = LuaValuetype.DATA_TYPES.get(type);
				if ( c != null ) {
					LuaValuetype o = null;
					try {
						Method method = c.getMethod("fromJSON", JSONObject.class);
						Object ot = method.invoke(null, temp);
						o = (LuaValuetype)ot;
					}catch( Exception e ) {
						e.printStackTrace();
					}
					
					return o;
				}
			}
		}
		
		return null;
	}
}
