/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.data;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import engine.lua.type.LuaValuetype;

public class Ray extends LuaValuetype {
	protected static final LuaValue C_ORIGIN = LuaValue.valueOf("Origin");
	protected static final LuaValue C_DIRECTION = LuaValue.valueOf("Direction");
	protected static final LuaValue C_UNIT = LuaValue.valueOf("Unit");
	
	@Override
	public String typename() {
		return "Ray";
	}
	
	public Ray(Vector3 origin, Vector3 direction) {
		this();

		((Vector3)this.rawget(C_ORIGIN)).setInternal(origin.getInternal());
		((Vector3)this.rawget(C_DIRECTION)).setInternal(direction.getInternal());
		((Vector3)this.rawget(C_UNIT)).setInternal(direction.getUnit().getInternal());
	}
	
	public Ray() {		
		// Define fields
		defineField(C_ORIGIN.toString(), new Vector3(0, 0, 0), true);
		defineField(C_DIRECTION.toString(), new Vector3(0, 0, 1), true);
		defineField(C_UNIT.toString(), new Vector3(0, 0, 1), true);
	}
	
	public LuaValue tostring() {
		return LuaValue.valueOf(typename()+":("+toString()+")");
	}
	
	@Override
	public boolean eq_b(LuaValue value) {
		return this.equals(value);
	}
	
	@Override
	public boolean equals(Object ray) {
		if ( ray == this )
			return true;
		
		if ( !(ray instanceof Ray) )
			return false;
		
		Ray vector3 = (Ray)ray;
		if ( !vector3.getOrigin().equals(this.getOrigin()) )
			return false;
		if ( !vector3.getDirection().equals(this.getDirection()) )
			return false;
		return true;
	}

	public Vector3 getOrigin() {
		return (Vector3) this.get(C_ORIGIN);
	}

	public Vector3 getDirection() {
		return (Vector3) this.get(C_DIRECTION);
	}

	public Vector3 getUnit() {
		return (Vector3) this.get(C_UNIT);
	}

	private static Ray newInstance(Vector3 origin, Vector3 direction) {
		return new Ray(origin, direction);
	}

	@Override
	protected LuaValue newInstanceFunction() {
		return new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2) {
				return newInstance((Vector3)arg1, (Vector3)arg2);
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSON() {
		JSONObject j = new JSONObject();
		//j.put("X", (double)this.getX());
		//j.put("Y", (double)this.getY());
		//j.put("Z", (double)this.getZ());
		return j;
	}
	
	public static Ray fromJSON(JSONObject json) {
		/*return newInstance(
			((Double)json.get("X")).doubleValue(),
			((Double)json.get("Y")).doubleValue(),
			((Double)json.get("Z")).doubleValue()
		);*/
		return new Ray();
	}
	
	@Override
	public LuaValuetype fromString(String input) {
		/*String[] t = input.replace(" ", "").split(",");
		if ( t.length != 3 )
			return this;
		
		Ray vec = new Ray(new Vector3f(
				(float) Double.parseDouble(t[0]),
				(float) Double.parseDouble(t[1]),
				(float) Double.parseDouble(t[2])
		));
		return vec;*/
		return new Ray();
	}

	@Override
	public LuaValuetype clone() {
		return newInstance(getOrigin(), getDirection());
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return null; // Unmodifiable from lua side
	}

	@Override
	protected boolean onValueGet(LuaValue key) {		
		return true;
	}

	@Override
	protected void onRegister(LuaTable table) {
		// Nothing to register
	}
}
