package engine.lua.type.data;

import org.joml.Vector2f;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.type.LuaValuetype;
import engine.util.Misc;

public class Vector2 extends LuaValuetype {
	protected static final LuaValue C_X = LuaValue.valueOf("X");
	protected static final LuaValue C_Y = LuaValue.valueOf("Y");
	protected static final LuaValue C_UNIT = LuaValue.valueOf("Unit");
	protected static final LuaValue C_MAGNITUDE = LuaValue.valueOf("Magnitude");
	
	private Vector2f internal;
	private boolean modified;
	
	@Override
	public String typename() {
		return "Vector2";
	}
	
	public Vector2(float x, float y) {
		this(new Vector2f(x, y));
	}
	
	public Vector2(Vector2f internal) {
		this();
		setInternal(internal);
	}
	
	public Vector2() {		
		// Define fields
		defineField(C_X.toString(), LuaValue.valueOf(0), true);
		defineField(C_Y.toString(), LuaValue.valueOf(0), true);
		this.internal = new Vector2f();
		
		// Create ToString function
		this.getmetatable().set("ToString", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf(Vector2.this.toString());
			}
		});

		// Create Dot function
		this.getmetatable().set("Dot", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue left, LuaValue right) {
				if (right instanceof Vector2) {
					Vector2 vec = (Vector2)right;
					return LuaValue.valueOf(getX() * vec.getX() + getY() * vec.getY());
				}
				return LuaValue.valueOf(0);
			}
		});
		this.getmetatable().set(LuaValue.EQ, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector2 left2 = getVector(left);
				Vector2 right2 = getVector(right);
				return LuaValue.valueOf(left2.equals(right2));
			}
		});

		this.getmetatable().set(LuaValue.ADD, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector2 left2 = getVector(left);
				Vector2 right2 = getVector(right);
				return newInstance(left2.getX() + right2.getX(), left2.getY() + right2.getY());
			}
		});

		this.getmetatable().set(LuaValue.SUB, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector2 left2 = getVector(left);
				Vector2 right2 = getVector(right);
				return newInstance(left2.getX() - right2.getX(), left2.getY() - right2.getY());
			}
		});

		this.getmetatable().set(LuaValue.MUL, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector2 left2 = getVector(left);
				Vector2 right2 = getVector(right);
				return newInstance(left2.getX() * right2.getX(), left2.getY() * right2.getY());
			}
		});

		this.getmetatable().set(LuaValue.DIV, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Vector2 left2 = getVector(left);
				Vector2 right2 = getVector(right);
				return newInstance(left2.getX() / right2.getX(), left2.getY() / right2.getY());
			}
		});
	}
	
	protected Vector2 getVector(LuaValue value) {
		if ( value instanceof Vector2 ) {
			return (Vector2)value;
		} else {
			if ( value.isnumber() ) {
				return newInstance( value.tofloat(), value.tofloat() );
			} else {
				LuaValue.error("Error casting value to type Vector2");
				return null;
			}
		}
	}

	public static Vector2 zero() {
		return Vector2.newInstance(0, 0);
	}
	
	public static Vector2 up() {
		return Vector2.newInstance(0, 1);
	}
	
	public static Vector2 down() {
		return Vector2.newInstance(0, -1);
	}
	
	public static Vector2 left() {
		return Vector2.newInstance(-1, 0);
	}
	
	public static Vector2 right() {
		return Vector2.newInstance(1, 0);
	}
	
	public float getX() {
		return internal.x;
	}

	public float getY() {
		return internal.y;
	}
	
	public String toString() {
		return Misc.truncateDecimal(getX(), 2) + ", " + Misc.truncateDecimal(getY(), 2);
	}
	
	public LuaValue tostring() {
		return LuaValue.valueOf(typename()+":("+toString()+")");
	}
	
	@Override
	public boolean equals(Object vector) {
		if ( vector == this )
			return true;
		
		if ( !(vector instanceof Vector2) )
			return false;
		
		if ( ((Vector2)vector).getX() != getX() )
			return false;
		
		if ( ((Vector2)vector).getY() != getY() )
			return false;
		
		return true;
	}

	private static Vector2 newInstance(double x, double y) {
		Vector2f internal = new Vector2f((float)x, (float)y);
		return new Vector2(internal);
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
	}

	@Override
	protected LuaValue newInstanceFunction() {
		return new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2) {
				return newInstance(arg1.todouble(), arg2.todouble());
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSON() {
		JSONObject j = new JSONObject();
		j.put("X", this.getX());
		j.put("Y", this.getY());
		return j;
	}
	
	public static Vector2 fromJSON(JSONObject json) {
		return newInstance(
			((Double)json.get("X")).doubleValue(),
			((Double)json.get("Y")).doubleValue()
		);
	}
	
	@Override
	public LuaValuetype fromString(String input) {
		String[] t = input.replace(" ", "").split(",");
		if ( t.length != 3 )
			return this;
		
		Vector2 vec = new Vector2(new Vector2f(
				(float) Double.parseDouble(t[0]),
				(float) Double.parseDouble(t[1])
		));
		return vec;
	}

	@Override
	public LuaValuetype clone() {
		return newInstance(getX(), getY());
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
				this.rawset(C_UNIT, new Vector2(new Vector2f(
						getX() / magnitude,
						getY() / magnitude
				)));
			}
		}
		
		return true;
	}

	public Vector2f toJoml() {
		return new Vector2f(internal);
	}

	public void setInternal(Vector2f internal) {
		this.rawset(C_X, LuaValue.valueOf(internal.x));
		this.rawset(C_Y, LuaValue.valueOf(internal.y));
		this.internal.set(internal);
		modified = true;
	}

	public Vector2 getUnit() {
		return (Vector2)this.get(C_UNIT);
	}
}
