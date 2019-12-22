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

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.type.LuaValuetype;

public class Matrix4 extends LuaValuetype {
	
	private Matrix4f internal;
	
	protected static final LuaValue C_P = LuaValue.valueOf("P");

	public Matrix4() {
		this(0, 0, 0);
	}
	
	public Matrix4(Matrix4 old) {
		this(new Matrix4f(old.internal));
	}
	
	public Matrix4(Vector3 old) {
		this(old.toJoml());
	}
	
	public Matrix4( double x, double y, double z ) {
		this(new Vector3f((float)x,(float)y,(float)z));
	}
	
	public Matrix4( Vector3f vector ) {
		this(new Matrix4f().identity().translate(vector));
	}
	
	public Matrix4(Matrix4f internal) {
		this.internal = new Matrix4f(internal);
		
		this.defineField("P", new Vector3(internal.getTranslation(new Vector3f())), false);

		// Create ToString function
		this.getmetatable().set("ToString", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf("Matrix4f()");
			}
		});
		
		this.getmetatable().set("Inverse", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return new Matrix4(new Matrix4f(internal).invert());
			}
		});
		
		this.getmetatable().set(LuaValue.MUL, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				if ( left instanceof Matrix4 && right instanceof Matrix4 ) {
					Matrix4 left2 = (Matrix4)left;
					Matrix4 right2 = (Matrix4)right;
					Matrix4 newMat = new Matrix4();
					newMat.internal = left2.internal.mul(right2.internal, newMat.internal);
					newMat.update();
					return newMat;
				}
				if ( left instanceof Matrix4 && right instanceof Vector3 ) {
					Matrix4 left2 = (Matrix4)left;
					Vector3 right2 = (Vector3)right;
					
					Vector3f vec = new Vector3f(right2.toJoml()).mulProject(left2.internal);
					Vector3 newVec = new Vector3(vec);
					return newVec;
				}
				LuaValue.error("Cannot multiply");
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(LuaValue.ADD, new TwoArgFunction() {

			@Override
			public LuaValue call(LuaValue left, LuaValue right) {
				if ( left instanceof Matrix4 && right instanceof Vector3 ) {
					Matrix4 left2 = (Matrix4)left;
					Vector3 right2 = (Vector3)right;
					
					Matrix4 newMat = new Matrix4(left2.internal);
					newMat.internal.translate(right2.toJoml());
					newMat.update();
					
					return newMat;
				}
				
				LuaValue.error("Cannot Add");
				return LuaValue.NIL;
			}
		});
		
		this.getmetatable().set(LuaValue.SUB, new TwoArgFunction() {

			@Override
			public LuaValue call(LuaValue left, LuaValue right) {
				if ( left instanceof Matrix4 && right instanceof Vector3 ) {
					Matrix4 left2 = (Matrix4)left;
					Vector3 right2 = (Vector3)right;
					
					Matrix4 newMat = new Matrix4(left2.internal);
					newMat.internal.translate(right2.toJoml().negate());
					newMat.update();
					
					return newMat;
				}
				
				LuaValue.error("Cannot Subtract");
				return LuaValue.NIL;
			}
		});
	}

	private void update() {
		Vector3f translation = internal.getTranslation(new Vector3f());
		Vector3 pos = (Vector3) this.rawget(C_P);
		pos.setInternal(translation);
	}

	@Override
	protected void onRegister(LuaTable table) {
		table.set("fromEulerAnglesXYZ", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				Matrix4f temp = new Matrix4f();
				temp.identity();
				float x = (float)args.arg(1).checkdouble();
				float y = (float)args.arg(2).checkdouble();
				float z = (float)args.arg(3).checkdouble();
				temp.rotateXYZ(x, y, z);
				return new Matrix4(temp);
			}
		});
	}

	/**
	 * Returns a clone of the internal joml-backed matrix.
	 * @return
	 */
	public Matrix4f toJoml() {
		return new Matrix4f(internal);
	}
	
	/**
	 * Returns the raw internal matrix.
	 * @return
	 */
	public Matrix4f getInternal() {
		return internal;
	}
	
	/**
	 * Return this matrix's translation.
	 * @return
	 */
	public Vector3 getPosition() { 
		return (Vector3) this.get(C_P);
	}
	
	/**
	 * Set this matrix's translation.
	 * @param value
	 */
	public void setPosition(Vector3 value) {
		this.rawset(C_P, value);
		this.set(C_P, value);
	}
	
	/**
	 * Forces the internal matrix to the desired matrix.
	 * @param matrix
	 */
	public void setInternal(Matrix4f matrix) {
		this.internal.set(matrix);
		update();
	}
	
	public boolean eq_b(LuaValue value) {
		return equals(value);
	}
	
	public boolean equals(Object o) {
		if ( o == this )
			return true;
		
		if ( !(o instanceof Matrix4) )
			return false;
		
		Matrix4f oi = ((Matrix4)o).internal;
		return oi.equals(internal);
	}

	protected LuaValue newInstanceFunction() {
		return new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				int amtarg = args.narg();
				
				try {
					// Blank Matrix
					if ( amtarg == 0 ) {
						return new Matrix4();
					}
					
					// Clone of matrix or set from Vector
					if ( amtarg == 1 ) {
						LuaValue t = args.arg(1);
						if ( t instanceof Matrix4 ) {
							return new Matrix4((Matrix4)t);
						}
						if ( t instanceof Vector3 ) {
							return new Matrix4((Vector3)t);
						}
					}
					
					// Build look-at matrix
					if ( amtarg == 2 ) {
						if ( args.arg(1) instanceof Vector3 && args.arg(2) instanceof Vector3 ) {
							Vector3 v1 = (Vector3)args.arg(1);
							Vector3 v2 = (Vector3)args.arg(2);
							float v1x = v1.getX();
							float v1y = v1.getY();
							float v1z = v1.getZ();
							float v2x = v2.getX();
							float v2y = v2.getY();
							float v2z = v2.getZ();
							if ( (v1x-v2x)*(v1x-v2x) + (v1y-v2y)*(v1y-v2y) + (v1z-v2z)*(v1z-v2z) < 0.0001 )
								return new Matrix4(v1);
							
							Matrix4f rotation = new Matrix4f()
									.rotateX((float)-Math.PI/2f)
									.lookAt(0, 0, 0, (v2x-v1x), (v2y-v1y), (v2z-v1z), 0, 0, 1)
									.rotateX((float)Math.PI)
									.rotateZ((float)Math.PI);
							Matrix4f source = new Matrix4f()
									.translate(v1x,v1y,v1z)
									.mul(rotation);
							return new Matrix4(source);
						}
						
						return new Matrix4();
					}
					
					// Build matrix from vector xyz
					if ( amtarg == 3 ) {
						double a1 = args.arg(1).checkdouble();
						double a2 = args.arg(2).checkdouble();
						double a3 = args.arg(3).checkdouble();
						
						return new Matrix4(a1, a2, a3);
					}
					LuaValue.error("Invalid constructor for " + this.typename());
				} catch(Exception e) {
					e.printStackTrace();
				}
				return LuaValue.NIL;
			}
		};
	}

	@Override
	public String typename() {
		return "Matrix4";
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.eq_b(C_P) ) {
			if ( value instanceof Vector3 ) {
				//this.setLocked(false);
				Vector3 vector = (Vector3)value;
				internal.m30(vector.getX());
				internal.m31(vector.getY());
				internal.m32(vector.getZ());
				//this.setLocked(true);
			}
		}

		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSON() {
		JSONObject j = new JSONObject();
		j.put("m00", (double)internal.m00());
		j.put("m01", (double)internal.m01());
		j.put("m02", (double)internal.m02());
		j.put("m03", (double)internal.m03());
		j.put("m10", (double)internal.m10());
		j.put("m11", (double)internal.m11());
		j.put("m12", (double)internal.m12());
		j.put("m13", (double)internal.m13());
		j.put("m20", (double)internal.m20());
		j.put("m21", (double)internal.m21());
		j.put("m22", (double)internal.m22());
		j.put("m23", (double)internal.m23());
		j.put("m30", (double)internal.m30());
		j.put("m31", (double)internal.m31());
		j.put("m32", (double)internal.m32());
		j.put("m33", (double)internal.m33());
		return j;
	}
	
	public LuaValuetype fromString(String input) {
		// Not implemented
		return this;
	}
	
	public static Matrix4 fromJSON(JSONObject json) {
		Matrix4f internal = new Matrix4f(
				(float)((Double)json.get("m00")).doubleValue(),
				(float)((Double)json.get("m01")).doubleValue(),
				(float)((Double)json.get("m02")).doubleValue(),
				(float)((Double)json.get("m03")).doubleValue(),
				(float)((Double)json.get("m10")).doubleValue(),
				(float)((Double)json.get("m11")).doubleValue(),
				(float)((Double)json.get("m12")).doubleValue(),
				(float)((Double)json.get("m13")).doubleValue(),
				(float)((Double)json.get("m20")).doubleValue(),
				(float)((Double)json.get("m21")).doubleValue(),
				(float)((Double)json.get("m22")).doubleValue(),
				(float)((Double)json.get("m23")).doubleValue(),
				(float)((Double)json.get("m30")).doubleValue(),
				(float)((Double)json.get("m31")).doubleValue(),
				(float)((Double)json.get("m32")).doubleValue(),
				(float)((Double)json.get("m33")).doubleValue()
			);
		return new Matrix4(internal);
	}

	@Override
	public LuaValuetype clone() {
		return new Matrix4(internal);
	}
}
