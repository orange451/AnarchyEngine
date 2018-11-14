package luaengine.type.data;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.util.Misc;
import luaengine.type.LuaValuetype;

public class Vector2 extends LuaValuetype {

	private float mag = 0;
	private LuaValue unit;
	private boolean modified = true;
	
	public Vector2() {
		this(new Vector2(null));
	}

	public Vector2(Vector2 unitVec) {
		this.unit = unitVec;
		
		defineField("X", LuaValue.valueOf(0), true);
		defineField("Y", LuaValue.valueOf(0), true);
		defineField("Magnitude", LuaValue.valueOf(0), true);
		defineField("Unit", unitVec==null?LuaValue.NIL:unitVec, true);

		// Create ToString function
		this.getmetatable().set("ToString", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf("( " + getX() + ", " + getY() + " )");
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

		// Create Cross function
		this.getmetatable().set("Cross", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue left, LuaValue right) {
				if (right instanceof Vector2) {
					return newInstance(getY(), -getX());
				}
				return LuaValue.NIL;
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

	@Override
	protected void onRegister(LuaTable table) {
		//
	}

	protected float getX() {
		return this.get("X").tofloat();
	}

	protected float getY() {
		return this.get("Y").tofloat();
	}

	protected float getMag() {
		return mag;
	}

	protected Vector2 getUnit() {
		return (Vector2) unit;
	}

	protected LuaValue newInstanceFunction() {
		return new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2) {
				return newInstance(arg1.tofloat(), arg2.tofloat());
			}
		};
	}

	public static Vector2 newInstance(float x, float y) {
		Vector2 inst = new Vector2(new Vector2(null));

		inst.getField("X").setLocked(false);
		inst.getField("Y").setLocked(false);
		inst.set("X", x);
		inst.set("Y", y);
		inst.getField("X").setLocked(true);
		inst.getField("Y").setLocked(true);
		return inst;
	}

	public static Vector2 getVector(LuaValue value) {
		if ( value instanceof Vector2 ) {
			return (Vector2)value;
		} else {
			return newInstance( value.tofloat(), value.tofloat() );
		}
	}

	@Override
	public String typename() {
		return "Vector2";
	}

	public LuaValuetype fromString(String input) {
		String[] t = input.replace(" ", "").split(",");
		if ( t.length != 2 )
			return this;
		
		this.rawset("X", LuaValue.valueOf(Double.parseDouble(t[0])));
		this.rawset("Y", LuaValue.valueOf(Double.parseDouble(t[1])));
		modified = true;
		return this;
	}
	
	public String toString() {
		return Misc.truncateDecimal(getX(), 2) + ", " + Misc.truncateDecimal(getY(), 2);
	}
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( unit == null ) {
			return null;
		}

		// If X or Y is changed. Update the Unit Vector
		if ( key.toString().equals("X") || key.toString().equals("Y") ) {
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
				mag = (float) Math.sqrt(X*X+Y*Y);
				this.rawset("Magnitude", LuaValue.valueOf(mag));
				if ( unit != null ) {
					unit.rawset("X", LuaValue.valueOf(X / mag));
					unit.rawset("Y", LuaValue.valueOf(Y / mag));
					this.rawset("Unit", unit);
				} else {
					this.rawset("Unit", this);
				}
			}
		}

		return true;
	}
	


	@Override
	public JSONObject toJSON() {
		JSONObject j = new JSONObject();
		j.put("X", this.getX());
		j.put("Y", this.getY());
		return j;
	}
	
	public static Vector2 fromJSON(JSONObject json) {
		return newInstance(
				(float)((Double)json.get("X")).doubleValue(),
				(float)((Double)json.get("Y")).doubleValue()
				);
	}

	@Override
	public LuaValuetype clone() {
		return newInstance(this.getX(), this.getY());
	}
}
