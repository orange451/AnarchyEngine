package engine.lua.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import engine.Game;
import engine.lua.LuaEngine;
import engine.lua.lib.Enums;
import engine.lua.type.object.Instance;

public abstract class LuaDatatype extends LuaTable {
	private HashMap<String,LuaField> fields = new HashMap<String,LuaField>();
	private ArrayList<LuaField> fieldsOrdered = new ArrayList<LuaField>();
	
	protected boolean locked = false;

	public void defineField(String fieldName, Object fieldValue, boolean isFinal) {
		if ( !this.containsField(fieldName) ) {
			// Create new field
			LuaField lf = new LuaField(fieldName, fieldValue.getClass(), isFinal);
			fields.put(fieldName, lf);
			fieldsOrdered.add(lf);
			
			if ( fieldValue.equals(LuaValue.NIL) || fieldValue instanceof Instance ) {
				lf.isInstance = true;
			}
		}

		// Set its value in the table
		LuaValue luaValue = null;
		if ( fieldValue instanceof LuaValue ) {
			luaValue = (LuaValue)fieldValue;
		} else {
			luaValue = CoerceJavaToLua.coerce(fieldValue);
		}
		this.rawset(fieldName, luaValue);
	}
	
	public void undefineField(String fieldName) {
		if ( !this.containsField(fieldName) )
			return;

		LuaField lf = fields.get(fieldName);
		lf.cleanup();
		this.fieldsOrdered.remove(lf);
		fields.remove(fieldName);
		
		this.rawset(fieldName, LuaValue.NIL);
		try{ this.set(fieldName, LuaValue.NIL); }catch(Exception e) {}
	}

	public boolean containsField(String fieldName) {
		return fields.containsKey(fieldName);
	}
	
	public LuaField getField(String fieldName) {
		return fields.get(fieldName);
	}
	
	public String[] getFields() {
		Set<String> keys = fields.keySet();
		String[] f = keys.toArray(new String[keys.size()]);
		return f;
	}
	
	public String[] getFieldsOrdered() {
		String[] t = new String[fieldsOrdered.size()];
		synchronized(fieldsOrdered) {
			for (int i = 0; i < t.length; i++) {
				t[i] = fieldsOrdered.get(i).getName();
			}
		}
		
		return t;
	}

	@Override
	public int type() {
		return LuaValue.TUSERDATA;
	}

	protected abstract LuaValue onValueSet( LuaValue key, LuaValue value );
	protected abstract boolean onValueGet( LuaValue key );
	
	protected void onValueUpdated(LuaValue key, LuaValue value) {
		// You can choose to overwrite this if you want...
	}

	/**
	 * Whether or not the instance is locked internally.<br>
	 * A locked instance cannot have its name, parent, or metatable changed.
	 * @param locked
	 */
	public void setLocked( boolean locked ) {
		this.locked = locked;
	}
	

	public boolean isLocked() {
		return this.locked;
	}

	public boolean hasKey( String key ) {
		if ( key.equals("Name") || key.equals("Parent") )
			return true;

		LuaValue[] keys = this.keys();
		for (int i = 0; i < keys.length; i++) {
			String name = keys[i].toString();
			if ( name.equals(key) ) {
				return true;
			}
		}
		
		if ( containsField(key) )
			return true;
		
		return false;
	}
	
	@Override
	public LuaValue setmetatable(LuaValue metatable) {
		if ( locked ) {
			return error("Cannot set metatable. Table is locked");
		}
		
		return super.setmetatable(metatable);
	}

	@Override
	public LuaValue get(LuaValue key) {
		if (!onValueGet(key)) {
			return NIL;
		}
		
		// If you're getting an instance field, but that instance is no longer in the game, return nil
		if ( Game.isLoaded() ) {
			if ( this.containsField(key.toString()) ) {
				LuaField field = this.getField(key.toString());
				if ( field.isInstance ) {
					LuaValue v = super.get(key);
					if ( v instanceof Instance ) {
						Instance inst = (Instance)v;
						if ( !inst.getName().equals("game") && inst.getParent().equals(NIL) ) {
							return NIL;
						}
					}
				}
			}
		}
		
		// Super get
		return super.get(key);
	}

	@Override
	public void set(LuaValue key, LuaValue value) {
		if (locked) {
			//LuaValue.error("Cannot set field " + key.toString() + " in type " + this.typename() + ". Table is locked.");
			//return;
			if ( key.toString().equals("Name") || key.toString().equals("Parent") ) {
				LuaValue.error("Cannot set field " + key.toString() + " in type " + this.typename() + ". Table is locked.");
				return;	
			}
		}

		// Check for fields
		if ( !hasKey(key.toString()) ) {
			LuaValue.error("Cannot create new field " + key.toString() + " in type " + this.typename());
		} else {
			boolean typeMismatch = true;
			LuaField f = this.getField(key.toString());
			
			// Modifying a field
			if ( f != null ) {
				// Cant be modified
				if ( !f.canModify() ) {
					LuaValue.error("Cannot set field " + key.toString() + ". Field is locked.");
					return;
				}
				
				// Check if its the right datatype
				if ( f.matches(value) )
					typeMismatch = false;
				if ( value instanceof Instance && f.isInstance )
					typeMismatch = false;
				
				// Type mismatch? Or missing field
				if ( typeMismatch ) {
					LuaValue.error("Cannot set field " + key.toString() + ". Type mismatch.");
					return;
				}
				
				// Clamp the value (will only clamp if a clamp was defined)
				LuaValue t = f.clamp(value);
				if ( t != value ) {
					value = t;
				}
				
				// If it needs to be an enum, check here
				if ( f.getEnumType() != null ) {
					LuaValue tab = LuaEngine.globals.get("Enum");
					LuaValue enu = tab.get(f.getEnumType().getType());
					
					if ( enu.get(value).isnil() ) {
						LuaValue.error("Cannot set field " + key.toString() + ". Enum type mismatch.");
					}
				}
			}
		}

		LuaValue newSet = value;
		if ( !key.toString().equals("Name") && !key.toString().equals("Parent") ) {
			newSet = onValueSet(key,value);
			if ( newSet == null ) {
				LuaValue.error("This value cannot be modified.");
				return;
			}
		}

		super.set(key, newSet);
		this.onValueUpdated(key, newSet);
		
		Game.changes = true;
	}
	
	public void cleanup() {
		this.setLocked(false);
		fields.clear();
		LuaValue[] keys = keys();
		for (int i = keys.length-1; i >= 0; i--) {
			this.rawset(keys[i], LuaValue.NIL);
		}
	}
}
