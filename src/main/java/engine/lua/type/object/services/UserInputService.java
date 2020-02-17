/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.services;

import java.util.HashMap;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.lwjgl.glfw.GLFW;

import engine.ClientEngine;
import engine.Game;
import engine.lua.lib.LuaTableReadOnly;
import engine.lua.type.LuaEvent;
import engine.lua.type.data.Vector2;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Camera;
import ide.layout.windows.icons.Icons;
import lwjgui.glfw.input.KeyboardHandler;
import lwjgui.glfw.input.MouseHandler;

public class UserInputService extends Service implements TreeViewable {

	private HashMap<Integer,Boolean> keysDown = new HashMap<Integer,Boolean>();
	private HashMap<Integer,Boolean> mouseDown = new HashMap<Integer,Boolean>();

	public static final LuaValue C_INPUTBEGAN = LuaValue.valueOf("InputBegan");
	public static final LuaValue C_INPUTENDED = LuaValue.valueOf("InputEnded");
	public static final LuaValue C_MOUSEPRESSED = LuaValue.valueOf("MousePressed");
	public static final LuaValue C_MOUSERELEASED = LuaValue.valueOf("MouseReleased");
	private static final LuaValue C_LOCKMOUSE = LuaValue.valueOf("LockMouse");
	
	public static boolean lockMouse;
	
	public UserInputService() {
		super("UserInputService");
		
		this.defineField(C_LOCKMOUSE, LuaValue.valueOf(false), false);

		this.rawset(C_INPUTBEGAN, new LuaEvent());
		this.rawset(C_INPUTENDED, new LuaEvent());
		this.rawset(C_MOUSEPRESSED, new LuaEvent());
		this.rawset(C_MOUSERELEASED, new LuaEvent());
		
		this.getmetatable().set("IsMouseLocked", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return LuaValue.valueOf(ClientEngine.engine.isMouseGrabbed());
			}
		});
		
		this.getmetatable().set("GetMovementVector", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue freeCam) {
				return getMovementVector(freeCam.toboolean());
			}
		});
		
		this.getmetatable().set("GetForwardVector", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue freeCam) {
				return getForwardVector(freeCam.toboolean());
			}
		});
		
		this.getmetatable().set("IsKeyDown", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				return isKeyDown(arg2.toint())?LuaValue.TRUE:LuaValue.FALSE;
			}
		});
		
		this.getmetatable().set("IsModifierDown", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				boolean isCtrlDown = isKeyDown(GLFW.GLFW_KEY_LEFT_SUPER)
									|| isKeyDown(GLFW.GLFW_KEY_RIGHT_SUPER)
									|| isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)
									|| isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
				return LuaValue.valueOf(isCtrlDown);
			}
		});
		
		this.getmetatable().set("IsMouseButtonDown", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				return isMouseDown(arg2.toint())?LuaValue.TRUE:LuaValue.FALSE;
			}
		});
		
		this.getmetatable().set("GetMouseDelta", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				MouseHandler mh = ClientEngine.renderThread.getWindow().getMouseHandler();
				return new Vector2(mh.getDX(), mh.getDY());
			}
		});
		
		this.setLocked(false);
	}
	
	public LuaEvent inputBeganEvent() {
		return (LuaEvent) this.get(C_INPUTBEGAN);
	}
	
	public LuaEvent inputEndedEvent() {
		return (LuaEvent) this.get(C_INPUTENDED);
	}
	
	public Vector3 getForwardVector(boolean freeCam) {
		Camera camera = Game.workspace().getCurrentCamera();
		if ( camera == null )
			return new Vector3();
		
		float yaw = (float) (camera.getYaw() - (Math.PI/2f));
		float pitch = freeCam?camera.getPitch():0;
		Vector3f forward = calculateDirectionVector(yaw, pitch);
		
		return new Vector3(forward);
	}
	
	public Vector3 getMovementVector(boolean freeCam) {
		Camera camera = Game.workspace().getCurrentCamera();
		if ( camera == null )
			return new Vector3();
		
		float yaw = (float) (camera.getYaw() - (Math.PI/2f));
		float pitch = freeCam?camera.getPitch():0;
		
		UserInputService uis = (UserInputService) Game.getService("UserInputService");
		boolean moveForward  = uis.isKeyDown( GLFW.GLFW_KEY_W );
		boolean moveBackward = uis.isKeyDown( GLFW.GLFW_KEY_S );
		boolean strafeLeft   = uis.isKeyDown( GLFW.GLFW_KEY_A );
		boolean strafeRight  = uis.isKeyDown( GLFW.GLFW_KEY_D );

		Vector3f newVelocity = new Vector3f();
		Vector3f forward  = calculateDirectionVector(yaw, pitch);
		Vector3f backward = calculateDirectionVector(yaw, pitch + (float)Math.PI);
		Vector3f right    = calculateDirectionVector(yaw - (float)Math.PI/2f, 0);
		Vector3f left     = calculateDirectionVector(yaw + (float)Math.PI/2f, 0);

		if (moveForward && strafeLeft == false && moveBackward == false && strafeRight == false) { newVelocity = forward; }
		if (moveForward == false && strafeLeft == false && moveBackward == true && strafeRight == false) { newVelocity = backward; }
		if (moveForward == false && strafeLeft == true && moveBackward == false && strafeRight == false) { newVelocity = left; }
		if (moveForward == false && strafeLeft == false && moveBackward == false && strafeRight == true) { newVelocity = right; }

		if (moveForward == true && strafeLeft == true && moveBackward == false && strafeRight == false) {
			forward.add(left, newVelocity).normalize();
		}
		if (moveForward == true && strafeLeft == false && moveBackward == false && strafeRight == true) {
			forward.add(right, newVelocity).normalize();
		}

		if (moveForward == false && strafeLeft == true && moveBackward == true && strafeRight == false) {
			backward.add(left, newVelocity).normalize();
		}
		if (moveForward == false && strafeLeft == false && moveBackward == true && strafeRight == true) {
			backward.add(right, newVelocity).normalize();
		}

		return new Vector3(newVelocity);
	}
	
	private Vector3f calculateDirectionVector( float yaw, float pitch ) {
		float x = (float) (Math.cos(-yaw)*Math.cos(pitch));
		float y = (float) (-Math.sin(-yaw)*Math.cos(pitch));
		float z = (float) (Math.sin(pitch));
		
		return new Vector3f( x, y, z );
	}
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.equals(C_LOCKMOUSE) ) {
			lockMouse = value.toboolean();
		}
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_keyboard;
	}
	
	public boolean isKeyDown(int key) {
		KeyboardHandler kh = ClientEngine.renderThread.getWindow().getKeyboardHandler();
		boolean system = kh.isKeyPressed(key);
		if ( !system )
			keysDown.remove(key);
		
		return keysDown.containsKey(key);
	}
	
	public boolean isMouseDown(int mouse) {
		MouseHandler mh = ClientEngine.renderThread.getWindow().getMouseHandler();
		boolean system = mh.isButtonPressed(mouse);
		if ( !system )
			mouseDown.remove(mouse);
		
		return mouseDown.containsKey(mouse);
	}

	public void onKeyPressed(int key) {
		if ( !keysDown.containsKey(key) )
			keysDown.put(key, true);
		((LuaEvent)this.rawget(C_INPUTBEGAN)).fire(new InputObjectKey(key));
	}
	
	public void onKeyReleased(int key) {
		keysDown.remove(key);
		((LuaEvent)this.rawget(C_INPUTENDED)).fire(new InputObjectKey(key));
	}

	public void onMousePress(int key) {
		if ( !mouseDown.containsKey(key) )
			mouseDown.put(key, true);
		((LuaEvent)this.rawget(C_MOUSEPRESSED)).fire(new InputObjectMouse(key));
		((LuaEvent)this.rawget(C_INPUTBEGAN)).fire(new InputObjectMouse(key));
	}
	
	public void onMouseRelease(int key) {
		mouseDown.remove(key);
		((LuaEvent)this.rawget(C_MOUSERELEASED)).fire(new InputObjectMouse(key));
		((LuaEvent)this.rawget(C_INPUTENDED)).fire(new InputObjectMouse(key));
	}
	
	public void onMouseScroll(int key) {
		((LuaEvent)this.rawget(C_INPUTBEGAN)).fire(new InputObjectMouse(key));
	}
}

class InputObjectKey extends LuaTableReadOnly {
	private static final LuaValue C_KEYCODE = LuaValue.valueOf("KeyCode");
	private static final LuaValue C_USERINPUTTYPE = LuaValue.valueOf("UserInputType");
	private static final LuaValue C_KEYBOARD = LuaValue.valueOf("Keyboard");
	
	public InputObjectKey(int keyCode) {
		this.rawset(C_KEYCODE, LuaValue.valueOf(keyCode));
		this.rawset(C_USERINPUTTYPE, C_KEYBOARD);
	}
}

class InputObjectMouse extends LuaTableReadOnly {
	private static final LuaValue C_BUTTON = LuaValue.valueOf("Button");
	private static final LuaValue C_USERINPUTTYPE = LuaValue.valueOf("UserInputType");
	private static final LuaValue C_MOUSE = LuaValue.valueOf("Mouse");
	
	public InputObjectMouse(int keyCode) {
		this.rawset(C_BUTTON, LuaValue.valueOf(keyCode));
		this.rawset(C_USERINPUTTYPE, C_MOUSE);
	}
}
