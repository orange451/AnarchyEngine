package engine.lua.lib;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.lwjgl.glfw.GLFW;

public class Enums extends TwoArgFunction {

	public LuaValue call(LuaValue modname, LuaValue env) {
		env.set("Enum", get());
		return env;
	}
	
	public static EnumTable get() {
		return new EnumTable();
	}

	/** --------------------- */
	/** The global Enum table */
	/** --------------------- */
	public static class EnumTable extends LuaTableReadOnly {
		public EnumTable() {
			
			// Shadow size
			this.rawset("TextureSize", new LuaTableReadOnly() {
				{
					this.rawset("256", 256);
					this.rawset("512", 512);
					this.rawset("1024", 1024);
					this.rawset("2048", 2048);
					this.rawset("4096", 4096);
				}
			});
			
			// Mouse Enum
			this.rawset("Mouse", new LuaTableReadOnly() {
				{
					this.rawset("Left", 0);
					this.rawset("Right", 1);
					this.rawset("Center", 2);
					this.rawset("WheelUp", 3);
					this.rawset("WheelDown", 4);
				}
			});
			
			// User Input Types Enum
			this.rawset("UserInputType", new LuaTableReadOnly() {
				{
					this.rawset("Keyboard", "Keyboard");
					this.rawset("Mouse", "Mouse");
				}
			});
			
			// Mouse Enum
			this.rawset("CameraType", new LuaTableReadOnly() {
				{
					this.rawset("Free", "Free");
					this.rawset("Scriptable", "Scriptable");
				}
			});
			
			// Position Enum
			this.rawset("PositionType", new LuaTableReadOnly() {
				{
					this.rawset("Absolute", "Absolute");
					this.rawset("Relative", "Relative");
				}
			});
			
			// Shape Enum
			this.rawset("Shape", new LuaTableReadOnly() {
				{
					this.rawset("Box",		"Box");
					this.rawset("Sphere",	"Sphere");
					this.rawset("Capsule",	"Capsule");
					this.rawset("Cylinder",	"Cylinder");
					this.rawset("Hull",		"Hull");
				}
			});
			
			// Keyboard
			this.rawset("KeyCode", new LuaTableReadOnly() {
				{
					this.rawset("Q", GLFW.GLFW_KEY_Q);
					this.rawset("W", GLFW.GLFW_KEY_W);
					this.rawset("E", GLFW.GLFW_KEY_E);
					this.rawset("R", GLFW.GLFW_KEY_R);
					this.rawset("T", GLFW.GLFW_KEY_T);
					this.rawset("Y", GLFW.GLFW_KEY_Y);
					this.rawset("U", GLFW.GLFW_KEY_U);
					this.rawset("I", GLFW.GLFW_KEY_I);
					this.rawset("O", GLFW.GLFW_KEY_O);
					this.rawset("P", GLFW.GLFW_KEY_P);

					this.rawset("A", GLFW.GLFW_KEY_A);
					this.rawset("S", GLFW.GLFW_KEY_S);
					this.rawset("D", GLFW.GLFW_KEY_D);
					this.rawset("F", GLFW.GLFW_KEY_F);
					this.rawset("G", GLFW.GLFW_KEY_G);
					this.rawset("H", GLFW.GLFW_KEY_H);
					this.rawset("J", GLFW.GLFW_KEY_J);
					this.rawset("K", GLFW.GLFW_KEY_K);
					this.rawset("L", GLFW.GLFW_KEY_L);
					
					this.rawset("Z", GLFW.GLFW_KEY_Z);
					this.rawset("X", GLFW.GLFW_KEY_X);
					this.rawset("C", GLFW.GLFW_KEY_C);
					this.rawset("V", GLFW.GLFW_KEY_V);
					this.rawset("B", GLFW.GLFW_KEY_B);
					this.rawset("N", GLFW.GLFW_KEY_N);
					this.rawset("M", GLFW.GLFW_KEY_M);
					
					this.rawset("Zero", 	GLFW.GLFW_KEY_0);
					this.rawset("One", 		GLFW.GLFW_KEY_1);
					this.rawset("Two", 		GLFW.GLFW_KEY_2);
					this.rawset("Three",	GLFW.GLFW_KEY_3);
					this.rawset("Four", 	GLFW.GLFW_KEY_4);
					this.rawset("Five", 	GLFW.GLFW_KEY_5);
					this.rawset("Six", 		GLFW.GLFW_KEY_6);
					this.rawset("Seven",	GLFW.GLFW_KEY_7);
					this.rawset("Eight",	GLFW.GLFW_KEY_8);
					this.rawset("Nine",		GLFW.GLFW_KEY_9);

					this.rawset("Space",		GLFW.GLFW_KEY_SPACE);
					this.rawset("Tab",			GLFW.GLFW_KEY_TAB);
					this.rawset("CapsLock",		GLFW.GLFW_KEY_CAPS_LOCK);
					this.rawset("LeftShift",	GLFW.GLFW_KEY_LEFT_SHIFT);
					this.rawset("RightShift",	GLFW.GLFW_KEY_RIGHT_SHIFT);
					this.rawset("Grave",		GLFW.GLFW_KEY_GRAVE_ACCENT);
					this.rawset("Minus",		GLFW.GLFW_KEY_MINUS);
					this.rawset("Equal",		GLFW.GLFW_KEY_EQUAL);
					this.rawset("Backspace",	GLFW.GLFW_KEY_BACKSPACE);
					this.rawset("Enter",		GLFW.GLFW_KEY_ENTER);
					this.rawset("LeftAlt",		GLFW.GLFW_KEY_LEFT_ALT);
					this.rawset("RightAlt",		GLFW.GLFW_KEY_RIGHT_ALT);
					this.rawset("RightControl",	GLFW.GLFW_KEY_RIGHT_CONTROL);
					this.rawset("LeftControl",	GLFW.GLFW_KEY_LEFT_CONTROL);
				}
			});
		}
	}
}