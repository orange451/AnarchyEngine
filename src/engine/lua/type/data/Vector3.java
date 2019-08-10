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
	protected static final LuaValue C_X = LuaValue.valueOf("X");
	protected static final LuaValue C_Y = LuaValue.valueOf("Y");
	protected static final LuaValue C_Z = LuaValue.valueOf("Z"); 
	protected static final LuaValue C_UNIT = LuaValue.valueOf("Unit");
	protected static final LuaValue C_MAGNITUDE = LuaValue.valueOf("Magnitude");
	
	private Vector3f internal;
	private boolean modified;
	
	@Override
	public String typename() {
		return "Vector3";
	}
	
	public Vector3(float x, float y, float z) {
		this(new Vector3f(x, y, z));
	}
	
	public Vector3(Vector3f internal) {
		this();
		setInternal(internal);
	}
	
	public Vector3() {		
		// Define fields
		defineField(C_X.toString(), LuaValue.valueOf(0), true);
		defineField(C_Y.toString(), LuaValue.valueOf(0), true);
		defineField(C_Z.toString(), LuaValue.valueOf(0), true);
		this.internal = new Vector3f();
		
		// Create ToString function
		this.getmetatable().set("ToString", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf(Vector3.this.toString());
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

		this.getmetatable().set(LuaValue.EQ, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector3 left2 = getVector(left);
				Vector3 right2 = getVector(right);
				return LuaValue.valueOf(left2.equals(right2));
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
	
	protected Vector3 getVector(LuaValue value) {
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

	public static Vector3 zero() {
		return Vector3.newInstance(0, 0, 0);
	}
	
	public static Vector3 up() {
		return Vector3.newInstance(0, 0, 1);
	}
	
	public static Vector3 down() {
		return Vector3.newInstance(0, 0, -1);
	}
	
	public static Vector3 left() {
		return Vector3.newInstance(-1, 0, 0);
	}
	
	public static Vector3 right() {
		return Vector3.newInstance(1, 0, 0);
	}
	
	public static Vector3 forward() {
		return Vector3.newInstance(0, 1, 0);
	}
	
	public static Vector3 backward() {
		return Vector3.newInstance(0, -1, 0);
	}
	
	public float getX() {
		return internal.x;
	}

	public float getY() {
		return internal.y;
	}

	public float getZ() {
		return internal.z;
	}
	
	public String toString() {
		return Misc.truncateDecimal(getX(), 2) + ", " + Misc.truncateDecimal(getY(), 2) + ", " + Misc.truncateDecimal(getZ(), 2);
	}
	
	public LuaValue tostring() {
		return LuaValue.valueOf(typename()+":("+toString()+")");
	}
	
	@Override
	public boolean equals(Object vector) {
		if ( vector == this )
			return true;
		
		if ( !(vector instanceof Vector3) )
			return false;
		
		if ( ((Vector3)vector).getX() != getX() )
			return false;
		
		if ( ((Vector3)vector).getY() != getY() )
			return false;
		
		if ( ((Vector3)vector).getZ() != getZ() )
			return false;
		
		return true;
	}

	private static Vector3 newInstance(double x, double y, double z) {
		Vector3f internal = new Vector3f((float)x, (float)y, (float)z);
		return new Vector3(internal);
	}

	@Override
	protected void onRegister(LuaTable table) {
		table.set("zero", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return zero();
			}
		});
		table.set("up", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return up();
			}
		});
		table.set("down", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return down();
			}
		});
		table.set("left", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return left();
			}
		});
		table.set("right", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return right();
			}
		});
		table.set("forward", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return forward();
			}
		});
		table.set("backward", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return backward();
			}
		});
	}

	@Override
	protected LuaValue newInstanceFunction() {
		return new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				return newInstance(arg1.todouble(), arg2.todouble(), arg3.todouble());
			}
		};
	}

	@SuppressWarnings("unchecked")
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
			((Double)json.get("X")).doubleValue(),
			((Double)json.get("Y")).doubleValue(),
			((Double)json.get("Z")).doubleValue()
		);
	}
	
	@Override
	public LuaValuetype fromString(String input) {
		String[] t = input.replace(" ", "").split(",");
		if ( t.length != 3 )
			return this;
		
		Vector3 vec = new Vector3(new Vector3f(
				(float) Double.parseDouble(t[0]),
				(float) Double.parseDouble(t[1]),
				(float) Double.parseDouble(t[2])
		));
		return vec;
	}

	@Override
	public LuaValuetype clone() {
		return newInstance(getX(), getY(), getZ());
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return null; // Unmodifiable
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		String keyName = key.toString();
		if ( keyName.equals(C_UNIT.toString()) || keyName.equals(C_MAGNITUDE.toString()) ) {
			if ( modified ) {
				modified = false;
				
				// Compute magnitude
				float magnitude = internal.length();
				this.rawset(C_MAGNITUDE, LuaValue.valueOf(magnitude));
				
				// Compute unit vector
				this.rawset(C_UNIT, new Vector3(new Vector3f(
						getX() / magnitude,
						getY() / magnitude,
						getZ() / magnitude
				)));
			}
		}
		
		return true;
	}

	public Vector3f toJoml() {
		return new Vector3f(internal);
	}

	public void setInternal(Vector3f internal) {
		this.rawset(C_X, LuaValue.valueOf(internal.x));
		this.rawset(C_Y, LuaValue.valueOf(internal.y));
		this.rawset(C_Z, LuaValue.valueOf(internal.z));
		this.internal.set(internal);
		modified = true;
	}

	public Vector3 getUnit() {
		return (Vector3)this.get(C_UNIT);
	}
}
