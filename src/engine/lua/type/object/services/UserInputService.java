package engine.lua.type.object.services;

import java.util.HashMap;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import engine.application.RenderableApplication;
import engine.lua.lib.LuaTableReadOnly;
import engine.lua.type.LuaEvent;
import engine.lua.type.data.Vector2;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Camera;
import ide.layout.windows.icons.Icons;

public class UserInputService extends Service implements TreeViewable {

	private HashMap<Integer,Boolean> keysDown = new HashMap<Integer,Boolean>();
	private HashMap<Integer,Boolean> mouseDown = new HashMap<Integer,Boolean>();
	
	public UserInputService() {
		super("UserInputService");
		
		this.defineField("LockMouse", LuaValue.valueOf(false), false);

		this.rawset("InputBegan", new LuaEvent());
		this.rawset("InputEnded", new LuaEvent());
		this.rawset("MousePressed", new LuaEvent());
		this.rawset("MouseReleased", new LuaEvent());
		
		this.getmetatable().set("GetMovementVector", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue arg1, LuaValue freeCam) {
				return getMovementVector(freeCam.toboolean());
			}
		});
		
		this.getmetatable().set("IsKeyDown", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue myself, LuaValue arg2) {
				return isKeyDown(arg2.toint())?LuaValue.TRUE:LuaValue.FALSE;
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
				return Vector2.newInstance((float)RenderableApplication.mouseDeltaX, (float)RenderableApplication.mouseDeltaY);
			}
		});
		
		this.setLocked(false);
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

		return new Vector3().setInternal(newVelocity);
	}
	
	private Vector3f calculateDirectionVector( float yaw, float pitch ) {
		float x = (float) (Math.cos(-yaw)*Math.cos(pitch));
		float y = (float) (-Math.sin(-yaw)*Math.cos(pitch));
		float z = (float) (Math.sin(pitch));
		
		return new Vector3f( x, y, z );
	}

	public LuaEvent getInputBeganEvent() {
		return (LuaEvent)this.rawget("InputBegan");
	}

	public LuaEvent getInputEndedEvent() {
		return (LuaEvent)this.rawget("InputEnded");
	}
	
	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		if ( key.toString().equals("LockMouse") ) {
			RenderableApplication.lockMouse = value.toboolean();
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
		boolean system = GLFW.glfwGetKey(RenderableApplication.window, key) == GLFW.GLFW_PRESS;
		if ( !system )
			keysDown.remove(key);
		
		return keysDown.containsKey(key);
	}
	
	public boolean isMouseDown(int mouse) {
		boolean system = GLFW.glfwGetMouseButton(RenderableApplication.window, mouse) == GLFW.GLFW_PRESS;
		if ( !system )
			mouseDown.remove(mouse);
		
		return mouseDown.containsKey(mouse);
	}

	public void onKeyPressed(int key) {
		if ( !keysDown.containsKey(key) )
			keysDown.put(key, true);
		((LuaEvent)this.rawget("InputBegan")).fire(new InputObject(key));
	}
	
	public void onKeyReleased(int key) {
		keysDown.remove(key);
		((LuaEvent)this.rawget("InputEnded")).fire(new InputObject(key));
	}

	public void onMousePress(int key) {
		if ( !mouseDown.containsKey(key) )
			mouseDown.put(key, true);
		((LuaEvent)this.rawget("MousePressed")).fire(new MouseInputObject(key));
	}
	
	public void onMouseRelease(int key) {
		mouseDown.remove(key);
		((LuaEvent)this.rawget("MouseReleased")).fire(new MouseInputObject(key));
	}
}

class InputObject extends LuaTableReadOnly {
	public InputObject(int keyCode) {
		this.rawset("KeyCode", keyCode);
	}
}

class MouseInputObject extends LuaTableReadOnly {
	public MouseInputObject(int button) {
		this.rawset("Button", button);
	}
}
