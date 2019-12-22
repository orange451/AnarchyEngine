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

import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import engine.lua.lib.EnumType;
import engine.lua.type.object.Instance;

public class LuaField {
	private String fieldName;
	private Class<?> fieldType;
	private boolean canModify;
	protected boolean isInstance;
	private Clamp<?> clamp;
	private EnumType enumType;

	public LuaField(String fieldName, Class<?> fieldType, boolean isFinal) {
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.canModify = !isFinal;
	}

	public boolean matches(Object o) {
		if ( o == null )
			return false;
		
		Class<? extends Object> cls = o.getClass();
		if ( o.equals(LuaValue.NIL) ) {
			if ( Instance.class.isAssignableFrom(fieldType) )
				return true;
		}
		if ( o instanceof Instance ) {
			if ( fieldType.equals(LuaValue.NIL.getClass()) )
				return true;
			if ( Instance.class.isAssignableFrom(fieldType) ) {
				return true;
			}
		}
		if ( o instanceof LuaValue ) {
			LuaValue v = ((LuaValue)o);
			if ( v.isnumber() && (fieldType.equals(LuaInteger.class) || fieldType.equals(LuaDouble.class) || fieldType.equals(LuaNumber.class))) {
				return true;
			}
		}
		if ( fieldType.equals(LuaString.class) && o instanceof LuaString ) {
			return true;
		}
		return cls.equals(fieldType);
	}
	
	public LuaValue clamp(LuaValue value) {
		if ( clamp == null )
			return value;
		
		return clamp.clamp(value);
	}

	public boolean canModify() {
		return canModify;
	}

	public String getName() {
		return this.fieldName;
	}

	public Class<?> getType() {
		if ( Instance.class.isAssignableFrom(fieldType) || isInstance )
			return Instance.class;
		
		return this.fieldType;
	}

	public void setLocked(boolean b) {
		this.canModify = !b;
	}

	public boolean isInstance() {
		return isInstance;
	}

	public void cleanup() {
		fieldName = null;
		fieldType = null;
	}

	public void setClamp(Clamp<?> clamp) {
		this.clamp = clamp;
	}
	
	public Clamp<?> getClamp() {
		return this.clamp;
	}

	public void setEnum(EnumType enumType) {
		this.enumType = enumType;
	}
	
	public EnumType getEnumType() {
		return this.enumType;
	}
}
