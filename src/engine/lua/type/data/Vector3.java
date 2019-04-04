package engine.lua.type.data;

import org.joml.Vector3f;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.type.LuaValuetype;
import engine.util.Misc;

public class Vector3 extends LuaValuetype {

	private double mag = 0;
	private LuaValue unit;
	private boolean modified = true;
	
	public Vector3() {
		this(new Vector3(null));
	}

	public Vector3(Vector3 unitVec) {
		this.unit = unitVec;

		// Create ToString function
		this.getmetatable().set("ToString", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf("( " + getX() + ", " + getY() + ", " + getZ() + " )");
			}
		});

		// Create Dot function
		this.getmetatable().set("Dot", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue left, LuaValue right) {
				if (right instanceof Vector3) {
					Vector3 vec = (Vector3)right;
					return LuaValue.valueOf(getX() * vec.getX() + getY() * vec.getY() + getZ() * vec.getZ());
				}
				return LuaValue.valueOf(0);
			}
		});

		// Create Cross function
		this.getmetatable().set("Cross", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue left, LuaValue right) {
				if (right instanceof Vector3) {
					Vector3 right2 = getVector(right);
					return newInstance( getY() * right2.getZ() - getZ() * right2.getZ(),
										getZ() * right2.getX() - getX() * right2.getZ(),
										getX() * right2.getY() - getY() * right2.getX());
				}
				return LuaValue.NIL;
			}
		});

		this.getmetatable().set(LuaValue.ADD, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector3 left2 = getVector(left);
				Vector3 right2 = getVector(right);
				return newInstance(left2.getX() + right2.getX(), left2.getY() + right2.getY(), left2.getZ() + right2.getZ());
			}
		});

		this.getmetatable().set(LuaValue.SUB, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector3 left2 = getVector(left);
				Vector3 right2 = getVector(right);
				return newInstance(left2.getX() - right2.getX(), left2.getY() - right2.getY(), left2.getZ() - right2.getZ());
			}
		});

		this.getmetatable().set(LuaValue.MUL, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector3 left2 = getVector(left);
				Vector3 right2 = getVector(right);
				return newInstance(left2.getX() * right2.getX(), left2.getY() * right2.getY(), left2.getZ() * right2.getZ());
			}
		});

		this.getmetatable().set(LuaValue.DIV, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector3 left2 = getVector(left);
				Vector3 right2 = getVector(right);
				return newInstance(left2.getX() / right2.getX(), left2.getY() / right2.getY(), left2.getZ() / right2.getZ());
			}
		});
	}

	@Override
	protected void onRegister(LuaTable table) {
		//
	}
	
	public Vector3 setInternal(Vector3f vector) {
		this.rawset("X", LuaValue.valueOf(vector.x));
		this.rawset("Y", LuaValue.valueOf(vector.y));
		this.rawset("Z", LuaValue.valueOf(vector.z));
		this.modified = true;
		return this;
	}

	public float getX() {
		return this.get("X").tofloat();
	}

	public float getY() {
		return this.get("Y").tofloat();
	}

	public float getZ() {
		return this.get("Z").tofloat();
	}

	protected double getMag() {
		return mag;
	}

	public Vector3 getUnit() {
		if ( unit.isnil() || !(unit instanceof Vector3) || ((Vector3)unit).isZero() )
			get("Unit");
		
		return (Vector3) unit;
	}

	private boolean isZero() {
		return getX() == 0 && getY() == 0 && getZ() == 0;
	}

	protected LuaValue newInstanceFunction() {
		return new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				return newInstance(arg1.tofloat(), arg2.tofloat(), arg3.tofloat());
			}
		};
	}

	public static Vector3 newInstance(float x, float y, float z) {
		Vector3 inst = new Vector3(new Vector3(null));
		inst.defineField("X", LuaValue.valueOf(x), true);
		inst.defineField("Y", LuaValue.valueOf(y), true);
		inst.defineField("Z", LuaValue.valueOf(z), true);
		inst.defineField("Magnitude", LuaValue.valueOf(inst.mag), true);
		inst.defineField("Unit", inst.unit, true);
		inst.modified = true;
		return inst;
	}

	public static Vector3 newInstance(Vector3f vector) {
		return newInstance( vector.x, vector.y, vector.z );
	}

	public static Vector3 newInstance(Vector3 vector) {
		return newInstance( vector.getX(), vector.getY(), vector.getZ() );
	}

	public static Vector3 getVector(LuaValue value) {
		if ( value instanceof Vector3 ) {
			return (Vector3)value;
		} else {
			if ( value.isnumber() ) {
				return newInstance( value.tofloat(), value.tofloat(), value.tofloat() );
			} else {
				LuaValue.error("Error casting value to type Vector3");
				return null;
			}
		}
	}

	@Override
	public String typename() {
		return "Vector3";
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( unit == null ) {
			return null;
		}

		// If X or Y is changed. Update the Unit Vector
		if ( key.toString().equals("X") || key.toString().equals("Y") || key.toString().equals("Z") ) {
			modified = true;
		}

		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		String keyName = key.toString();
		if ( keyName.equals("Unit") || keyName.equals("Magnitude") ) {
			if ( modified ) {
				modified = false;
				float X = getX();
				float Y = getY();
				float Z = getZ();
				mag = Math.sqrt(X*X+Y*Y+Z*Z);
				this.rawset("Magnitude", LuaValue.valueOf(mag));
				if ( unit != null ) {
					unit.rawset("X", LuaValue.valueOf(X / mag));
					unit.rawset("Y", LuaValue.valueOf(Y / mag));
					unit.rawset("Z", LuaValue.valueOf(Z / mag));
					this.rawset("Unit", unit);
				} else {
					this.rawset("Unit", this);
				}
			}
		}
		return true;
	}

	public Vector3f toJoml() {
		return new Vector3f(getX(), getY(), getZ());
	}
	
	public String toString() {
		return Misc.truncateDecimal(getX(), 2) + ", " + Misc.truncateDecimal(getY(), 2) + ", " + Misc.truncateDecimal(getZ(), 2);
	}
	
	public LuaValuetype fromString(String input) {
		String[] t = input.replace(" ", "").split(",");
		if ( t.length != 3 )
			return this;
		
		this.rawset("X", LuaValue.valueOf(Double.parseDouble(t[0])));
		this.rawset("Y", LuaValue.valueOf(Double.parseDouble(t[1])));
		this.rawset("Z", LuaValue.valueOf(Double.parseDouble(t[2])));
		modified = true;
		return this;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject j = new JSONObject();
		j.put("X", this.getX());
		j.put("Y", this.getY());
		j.put("Z", this.getZ());
		return j;
	}
	
	public static Vector3 fromJSON(JSONObject json) {
		return newInstance(
				(float)((Double)json.get("X")).doubleValue(),
				(float)((Double)json.get("Y")).doubleValue(),
				(float)((Double)json.get("Z")).doubleValue()
				);
	}

	@Override
	public LuaValuetype clone() {
		return newInstance(getX(), getY(), getZ());
	}
}
