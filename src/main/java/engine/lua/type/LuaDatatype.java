/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.LuaEngine;
import engine.lua.type.object.Instance;

public abstract class LuaDatatype extends LuaTable {
	private HashMap<LuaValue,LuaField> fields = new HashMap<LuaValue,LuaField>();
	private ArrayList<LuaField> fieldsOrdered = new ArrayList<LuaField>();
	
	protected boolean locked = false;

	/**
	 * Define a field for this Datatype. Deprecated. See {@link #defineField(LuaValue, LuaValue, boolean)}.
	 * @param fieldName
	 * @param fieldValue
	 * @param isFinal
	 * @return
	 */
	@Deprecated
	public LuaField defineField(String fieldName, LuaValue fieldValue, boolean isFinal) {
		return defineField(LuaValue.valueOf(fieldName), fieldValue, isFinal);
	}

	/**
	 * Define a field for this Datatype.
	 * @param fieldName
	 * @param fieldValue
	 * @param isFinal
	 * @return
	 */
	public LuaField defineField(LuaValue fieldName, LuaValue fieldValue, boolean isFinal) {
		LuaField ret = this.getField(fieldName);
		if ( ret == null ) {
			// Create new field
			ret = new LuaField(fieldName.toString(), fieldValue.getClass(), isFinal);
			fields.put(fieldName, ret);
			fieldsOrdered.add(ret);
			
			if ( fieldValue.equals(LuaValue.NIL) || fieldValue instanceof Instance )
				ret.isInstance = true;
		}

		// Set its value in the table
		this.rawset(fieldName, fieldValue);
		return ret;
	}
	
	/**
	 * Undefines a filed in the Datatype.
	 * @param fieldName
	 */
	public void undefineField(LuaValue fieldName) {
		if ( !this.containsField(fieldName) )
			return;

		LuaField lf = fields.get(fieldName);
		lf.cleanup();
		this.fieldsOrdered.remove(lf);
		fields.remove(fieldName);
		
		this.rawset(fieldName, LuaValue.NIL);
		try{ this.set(fieldName, LuaValue.NIL); }catch(Exception e) {}
	}

	/**
	 * Returns whether a field is defined in this Datatype.
	 * @param key
	 * @return
	 */
	public boolean containsField(LuaValue key) {
		return fields.containsKey(key);
	}
	
	/**
	 * Gets the field object for the specified field name.
	 * @param key
	 * @return
	 */
	public LuaField getField(LuaValue key) {
		return fields.get(key);
	}
	
	/**
	 * Gets the field object for the specified field name.
	 * @param key
	 * @return
	 */
	public LuaField getField(String key) {
		return fields.get(LuaValue.valueOf(key));
	}
	
	/**
	 * Returns list of fields in the order they are defined.
	 * @return
	 */
	public LuaField[] getFields() {
		Collection<LuaField> keys = fields.values();
		LuaField[] f = keys.toArray(new LuaField[keys.size()]);
		return f;
	}
	
	/**
	 * Returns list of fields in the order they are defined.
	 * @return
	 */
	public LuaValue[] getFieldNames() {
		Set<LuaValue> keys = fields.keySet();
		LuaValue[] f = keys.toArray(new LuaValue[keys.size()]);
		return f;
	}
	
	/**
	 * Returns list of fields ordered.
	 * @return
	 */
	public LuaValue[] getFieldNamesOrdered() {
		LuaValue[] t = new LuaValue[fieldsOrdered.size()];
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
	
	/**
	 * Returns whether or not the instance is locked internally. See {@link #setLocked(boolean)}.
	 * @return
	 */
	public boolean isLocked() {
		return this.locked;
	}

	private static final LuaValue C_NAME = LuaValue.valueOf("Name");
	private static final LuaValue C_PARENT = LuaValue.valueOf("Parent");
	private static final LuaValue C_ENUM = LuaValue.valueOf("Enum");
	
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
			LuaField field = this.getField(key);
			if ( field != null ) {
				if ( field.isInstance ) {
					LuaValue v = super.get(key);
					if ( v instanceof Instance ) {
						Instance inst = (Instance)v;
						if ( inst.destroyed )
							return NIL;
						//if ( !inst.getName().equals("game") && inst.getParent().equals(NIL) ) {
						//if ( !inst.equals(Game.game()) && inst.rawget(C_PARENT).eq_b(LuaValue.NIL) ) {
							//return NIL;
						//}
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
			if ( key.eq_b(C_NAME) || key.eq_b(C_PARENT) ) {
				LuaValue.error("Cannot set field " + key.toString() + " in type " + this.typename() + ". Table is locked.");
				return;	
			}
		}

		// Check for fields
		LuaField f = this.getField(key);
		if ( f == null) {
			LuaValue.error("Cannot create new field " + key.toString() + " in type " + this.typename());
		} else {
			boolean typeMismatch = true;
			
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
				LuaValue.error("Cannot set field " + key.toString() + ". Type mismatch. Expects: " + f.getType().getSimpleName());
				return;
			}
			
			// Clamp the value (will only clamp if a clamp was defined)
			LuaValue t = f.clamp(value);
			if ( t != value ) {
				value = t;
			}
			
			// If it needs to be an enum, check here
			if ( f.getEnumType() != null ) {
				LuaValue tab = LuaEngine.globals.get(C_ENUM);
				LuaValue enu = tab.get(f.getEnumType().getType());
				
				if ( enu.get(value).isnil() ) {
					LuaValue.error("Cannot set field " + key.toString() + ". Enum type mismatch.");
					return;
				}
			}
		}

		LuaValue newSet = value;
		if ( !key.eq_b(C_NAME) && !key.eq_b(C_PARENT) ) {
			newSet = onValueSet(key,value);
			if ( newSet == null ) {
				LuaValue.error("This value cannot be modified.");
				return;
			}
		}

		super.set(key, newSet);
	}
	
	public void cleanup() {
		
		// Lock it, can no longer be modified.
		this.setLocked(true);
		fields.clear();
		
		// Delete all its keys
		LuaValue[] keys = keys();
		for (int i = keys.length-1; i >= 0; i--) {
			this.rawset(keys[i], LuaValue.NIL);
		}
		
		// Force it to be weak (garbage collection)
		this.getmetatable().set( LuaValue.MODE, LuaValue.valueOf("kv") );
	}
}
