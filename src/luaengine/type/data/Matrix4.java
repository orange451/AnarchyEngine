package luaengine.type.data;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import luaengine.type.LuaValuetype;

public class Matrix4 extends LuaValuetype {
	
	private Matrix4f internal;

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
		
		this.defineField("P", Vector3.newInstance(0,0,0), false);
		update();

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
					Vector3 newVec = Vector3.newInstance(vec.x,vec.y,vec.z);
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
		Vector3 pos = (Vector3) this.rawget("P");
		Vector3f translation = internal.getTranslation(new Vector3f());
		pos.rawset("X", translation.x);
		pos.rawset("Y", translation.y);
		pos.rawset("Z", translation.z);
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
		return (Vector3) this.get("P");
	}
	
	/**
	 * Set this matrix's translation.
	 * @param value
	 */
	public void setPosition(Vector3 value) {
		this.rawset("P", value);
		this.set("P", value);
	}
	
	/**
	 * Forces the internal matrix to the desired matrix.
	 * @param matrix
	 */
	public void setInternal(Matrix4f matrix) {
		this.internal.set(matrix);
		update();
	}

	protected LuaValue newInstanceFunction() {
		return new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				int amtarg = args.narg();
				if ( amtarg == 0 ) {
					return new Matrix4();
				}
				if ( amtarg == 1 ) {
					LuaValue t = args.arg(1);
					if ( t instanceof Matrix4 ) {
						return new Matrix4((Matrix4)t);
					}
					if ( t instanceof Vector3 ) {
						return new Matrix4((Vector3)t);
					}
				}
				if ( amtarg == 3 ) {
					double a1 = args.arg(1).checkdouble();
					double a2 = args.arg(2).checkdouble();
					double a3 = args.arg(3).checkdouble();
					
					return new Matrix4(a1, a2, a3);
				}
				LuaValue.error("Invalid constructor for " + this.typename());
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
		if ( key.toString().equals("P") ) {
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
		j.put("m00", internal.m00());
		j.put("m01", internal.m01());
		j.put("m02", internal.m02());
		j.put("m03", internal.m03());
		j.put("m10", internal.m10());
		j.put("m11", internal.m11());
		j.put("m12", internal.m12());
		j.put("m13", internal.m13());
		j.put("m20", internal.m20());
		j.put("m21", internal.m21());
		j.put("m22", internal.m22());
		j.put("m23", internal.m23());
		j.put("m30", internal.m30());
		j.put("m31", internal.m31());
		j.put("m32", internal.m32());
		j.put("m33", internal.m33());
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
