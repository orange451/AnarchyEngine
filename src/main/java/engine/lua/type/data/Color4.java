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

import org.joml.Vector4f;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import engine.lua.lib.FourArgFunction;
import engine.lua.type.LuaValuetype;
import lwjgui.paint.Color;

public class Color4 extends ColorBase {

	private Color internal;
	
	public Color4(Color color) {
		this();
		
		this.rawset("R", LuaValue.valueOf(color.getRed()));
		this.rawset("G", LuaValue.valueOf(color.getGreen()));
		this.rawset("B", LuaValue.valueOf(color.getBlue()));
		this.rawset("A", LuaValue.valueOf(color.getAlphaF()));
	}
	
	public Color4(Color4 color) {
		this(color.internal);
	}
	
	public Color4() {
		
		// Create ToString function
		this.getmetatable().set("ToString", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf(Color4.this.toString());
			}
		});
		
		this.getmetatable().set(LuaValue.ADD, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Color4 left2 = getColor(left);
				Color4 right2 = getColor(right);
				return newInstance(left2.getR() + right2.getR(), left2.getG() + right2.getG(), left2.getB() + right2.getB(), left2.getA() + right2.getA());
			}
		});
		
		this.getmetatable().set(LuaValue.SUB, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Color4 left2 = getColor(left);
				Color4 right2 = getColor(right);
				return newInstance(left2.getR() - right2.getR(), left2.getG() - right2.getG(), left2.getB() - right2.getB(), left2.getA() - right2.getA());
			}
		});
		
		this.getmetatable().set(LuaValue.MUL, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Color4 left2 = getColor(left);
				Color4 right2 = getColor(right);
				return newInstance(
					(int)(((left2.getR()/255f) * (right2.getR()/255f)) * 255),
					(int)(((left2.getG()/255f) * (right2.getG()/255f)) * 255),
					(int)(((left2.getB()/255f) * (right2.getB()/255f)) * 255),
					left2.getA() * right2.getA()
				);
			}
		});
		
		this.getmetatable().set(LuaValue.DIV, new TwoArgFunction() {
			public LuaValue call(LuaValue left, LuaValue right) {
				Color4 left2 = getColor(left);
				Color4 right2 = getColor(right);
				return newInstance(
						(int)(((left2.getR()/255f) / (right2.getR()/255f)) * 255),
						(int)(((left2.getG()/255f) / (right2.getG()/255f)) * 255),
						(int)(((left2.getB()/255f) / (right2.getB()/255f)) * 255),
						left2.getA() / right2.getA()
					);
			}
		});
		
		// Define fields
		defineField("R", LuaValue.valueOf(0), true);
		defineField("G", LuaValue.valueOf(0), true);
		defineField("B", LuaValue.valueOf(0), true);
		defineField("A", LuaValue.valueOf(0), true);
	}
	
	private Color4 getColor(LuaValue value) {
		if ( value instanceof Color4 )
			return ((Color4)value);
		
		if ( value.isnumber() ) {
			int t = (int) (value.tofloat() * 255);
			return newInstance(t, t, t, 1);
		}
		
		// Cant get color...
		LuaValue.error("Error casting value to type " + this.typename());
		return null;
	}

	public static Color4 red() {
		return Color4.newInstance(255, 0, 0, 1);
	}
	
	public static Color4 green() {
		return Color4.newInstance(0, 255, 0, 1);
	}
	
	public static Color4 blue() {
		return Color4.newInstance(0, 0, 255, 1);
	}
	
	public static Color4 white() {
		return Color4.newInstance(255, 255, 255, 1);
	}
	
	public static Color4 black() {
		return Color4.newInstance(0, 0, 0, 1);
	}
	
	public static Color4 gray() {
		return Color4.newInstance(128, 128, 128, 1);
	}
	
	public static Color4 darkgray() {
		return Color4.newInstance(64, 64, 64, 1);
	}
	
	public static Color4 lightgray() {
		return Color4.newInstance(192, 192, 192, 1);
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
	
	@Override
	public Color toColor() {
		if ( internal == null )
			internal = new Color(getR()/255f, getG()/255f, getB()/255f, getA());
		
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

	public float getA() {
		return Math.max(0, Math.min(1, this.get("A").tofloat() ));
	}

	protected LuaValue newInstanceFunction() {
		return new FourArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3, LuaValue arg4) {
				return newInstance(arg1.toint(), arg2.toint(), arg3.toint(), arg4.tofloat());
			}
		};
	}

	public static Color4 newInstance(int r, int g, int b, float a) {
		Color4 inst = new Color4();
		inst.rawset("R", LuaValue.valueOf(r));
		inst.rawset("G", LuaValue.valueOf(g));
		inst.rawset("B", LuaValue.valueOf(b));
		inst.rawset("A", LuaValue.valueOf(a));
		return inst;
	}

	@Override
	public String typename() {
		return "Color4";
	}
	
	public String toString() {
		return getR() + ", " + getG() + ", " + getB() + ", " + getA();
	}

	
	public LuaValue tostring() {
		return LuaValue.valueOf(typename()+":("+toString()+")");
	}
	
	public LuaValuetype fromString(String input) {
		String[] t = input.replace(" ", "").split(",");
		if ( t.length != 4 )
			return this;
		this.rawset("R", LuaValue.valueOf(Integer.parseInt(t[0])));
		this.rawset("G", LuaValue.valueOf(Integer.parseInt(t[1])));
		this.rawset("B", LuaValue.valueOf(Integer.parseInt(t[2])));
		this.rawset("A", LuaValue.valueOf(Float.parseFloat(t[3])));
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
		j.put("R", (long)this.getR());
		j.put("G", (long)this.getG());
		j.put("B", (long)this.getB());
		j.put("A", (double)this.getA());
		return j;
	}
	
	public static Color4 fromJSON(JSONObject json) {
		return newInstance(
				(int)((Number)json.get("R")).intValue(),
				(int)((Number)json.get("G")).intValue(),
				(int)((Number)json.get("B")).intValue(),
				(float)((Number)json.get("A")).floatValue()
				);
	}

	@Override
	public LuaValuetype clone() {
		return newInstance(this.getR(), this.getG(), this.getB(), this.getA());
	}

	public Vector4f toJOML() {
		return new Vector4f( getR()/255f, getG()/255f, getB()/255f, getA() );
	}
}
