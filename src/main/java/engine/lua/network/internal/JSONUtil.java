package engine.lua.network.internal;

import java.lang.reflect.Method;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.LuaValuetype;
import engine.lua.type.object.Instance;

public class JSONUtil {
	private static final String C_TYPE = "Type";
	private static final String C_VALUE = "Value";
	private static final String C_REFERENCE = "Reference";
	private static final String C_CLASSNAME = "ClassName";
	private static final String C_DATATYPE = "Datatype";
	private static final String C_DATA = "Data";
	
	/**
	 * Serializes a field value to JSON.
	 * @param luaValue
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Object serializeField(LuaValue luaValue) {
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
			j.put(C_TYPE, C_REFERENCE);
			j.put(C_VALUE, refId);
			return j;
		}

		// Vector3/Colors/etc
		if ( luaValue instanceof LuaValuetype ) {
			JSONObject t = new JSONObject();
			t.put(C_CLASSNAME, ((LuaValuetype)luaValue).typename());
			t.put(C_DATA, ((LuaValuetype)luaValue).toJSON());

			JSONObject j = new JSONObject();
			j.put(C_TYPE, C_DATATYPE);
			j.put(C_VALUE, t);
			return j;
		}

		return null;
	}
	
	/**
	 * Deserializes JSON into a field value.
	 * @param t
	 * @return
	 */
	public static LuaValue deserializeField(Object t) {
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
			
			if ( j.get(C_TYPE).equals(C_REFERENCE) ) {
				long v = Long.parseLong(j.get(C_VALUE).toString());
				return Game.getInstanceFromSID(Game.game(), v);
			}
			
			if ( j.get(C_TYPE).equals(C_DATATYPE) ) {
				JSONObject data = (JSONObject) j.get(C_VALUE);
				String type = (String) data.get(C_CLASSNAME);
				JSONObject temp = (JSONObject) data.get(C_DATA);
				
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
