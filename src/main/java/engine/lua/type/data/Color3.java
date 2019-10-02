package engine.lua.type.data;

import org.joml.Vector3f;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.type.LuaValuetype;
import lwjgui.paint.Color;

public class Color3 extends LuaValuetype {

	private Color internal;
	
	public Color3() {
		
		// Create ToString function
		this.getmetatable().set("ToString", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf(Color3.this.toString());
			}
		});
		
		this.getmetatable().set(LuaValue.ADD, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Color3 left2 = getColor(left);
				Color3 right2 = getColor(right);
				return newInstance(left2.getR() + right2.getR(), left2.getG() + right2.getG(), left2.getB() + right2.getB());
			}
		});
		
		this.getmetatable().set(LuaValue.SUB, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Color3 left2 = getColor(left);
				Color3 right2 = getColor(right);
				return newInstance(left2.getR() - right2.getR(), left2.getG() - right2.getG(), left2.getB() - right2.getB());
			}
		});
		
		this.getmetatable().set(LuaValue.MUL, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Color3 left2 = getColor(left);
				Color3 right2 = getColor(right);
				return newInstance(
					(int)(((left2.getR()/255f) * (right2.getR()/255f)) * 255),
					(int)(((left2.getG()/255f) * (right2.getG()/255f)) * 255),
					(int)(((left2.getB()/255f) * (right2.getB()/255f)) * 255)
				);
			}
		});
		
		this.getmetatable().set(LuaValue.DIV, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Color3 left2 = getColor(left);
				Color3 right2 = getColor(right);
				return newInstance(
						(int)(((left2.getR()/255f) / (right2.getR()/255f)) * 255),
						(int)(((left2.getG()/255f) / (right2.getG()/255f)) * 255),
						(int)(((left2.getB()/255f) / (right2.getB()/255f)) * 255)
					);
			}
		});
		
		// Define fields
		defineField("R", LuaValue.valueOf(0), true);
		defineField("G", LuaValue.valueOf(0), true);
		defineField("B", LuaValue.valueOf(0), true);
	}
	
	private Color3 getColor(LuaValue value) {
		if ( value instanceof Color3 )
			return ((Color3)value);
		
		if ( value.isnumber() ) {
			int t = (int) (value.tofloat() * 255);
			return newInstance(t, t, t);
		}
		
		// Cant get color...
		LuaValue.error("Error casting value to type Vector3");
		return null;
	}

	public static Color3 red() {
		return Color3.newInstance(255, 0, 0);
	}
	
	public static Color3 green() {
		return Color3.newInstance(0, 255, 0);
	}
	
	public static Color3 blue() {
		return Color3.newInstance(0, 0, 255);
	}
	
	public static Color3 white() {
		return Color3.newInstance(255, 255, 255);
	}
	
	public static Color3 black() {
		return Color3.newInstance(0, 0, 0);
	}
	
	public static Color3 gray() {
		return Color3.newInstance(128, 128, 128);
	}
	
	public static Color3 darkgray() {
		return Color3.newInstance(64, 64, 64);
	}
	
	public static Color3 lightgray() {
		return Color3.newInstance(192, 192, 192);
	}

	@Override
	protected void onRegister(LuaTable table) {
		table.set("red", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return red();
			}
		});
		table.set("green", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return green();
			}
		});
		table.set("blue", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return blue();
			}
		});
		table.set("white", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return white();
			}
		});
		table.set("black", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return black();
			}
		});
		table.set("gray", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return gray();
			}
		});
		table.set("darkgray", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return darkgray();
			}
		});
		table.set("lightgray", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return lightgray();
			}
		});
	}
	
	public Color toColor() {
		if ( internal == null )
			internal = new Color(getR(), getG(), getB());
		
		return internal;
	}

	public int getR() {
		return Math.max(0, Math.min(255, this.get("R").checkint() ));
	}

	public int getG() {
		return Math.max(0, Math.min(255, this.get("G").checkint() ));
	}

	public int getB() {
		return Math.max(0, Math.min(255, this.get("B").checkint() ));
	}

	protected LuaValue newInstanceFunction() {
		return new ThreeArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
				return newInstance(arg1.toint(), arg2.toint(), arg3.toint());
			}
		};
	}

	public static Color3 newInstance(int r, int g, int b) {
		Color3 inst = new Color3();
		inst.rawset("R", LuaValue.valueOf(r));
		inst.rawset("G", LuaValue.valueOf(g));
		inst.rawset("B", LuaValue.valueOf(b));
		return inst;
	}

	public static Color3 newInstance(Color color) {
		Color3 inst = new Color3();
		inst.rawset("R", LuaValue.valueOf(color.getRed()));
		inst.rawset("G", LuaValue.valueOf(color.getGreen()));
		inst.rawset("B", LuaValue.valueOf(color.getBlue()));
		return inst;
	}

	@Override
	public String typename() {
		return "Color3";
	}
	
	public String toString() {
		return getR() + ", " + getG() + ", " + getB();
	}

	
	public LuaValue tostring() {
		return LuaValue.valueOf(typename()+":("+toString()+")");
	}
	
	public LuaValuetype fromString(String input) {
		String[] t = input.replace(" ", "").split(",");
		if ( t.length != 3 )
			return this;
		this.rawset("R", LuaValue.valueOf(Integer.parseInt(t[0])));
		this.rawset("G", LuaValue.valueOf(Integer.parseInt(t[1])));
		this.rawset("B", LuaValue.valueOf(Integer.parseInt(t[2])));
		internal = null;
		return this;
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
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
		j.put("R", this.getR());
		j.put("G", this.getG());
		j.put("B", this.getB());
		return j;
	}
	
	public static Color3 fromJSON(JSONObject json) {
		return newInstance(
				(int)((Long)json.get("R")).intValue(),
				(int)((Long)json.get("G")).intValue(),
				(int)((Long)json.get("B")).intValue()
				);
	}

	@Override
	public LuaValuetype clone() {
		return newInstance(this.getR(), this.getG(), this.getB());
	}

	public Vector3f toJOML() {
		return new Vector3f( getR()/255f, getG()/255f, getB()/255f );
	}
}
