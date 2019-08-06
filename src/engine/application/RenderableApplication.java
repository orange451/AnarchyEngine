package engine.application;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import engine.application.launchstrategy.ClientLauncher;
import engine.gl.Pipeline;
import engine.gl.Resources;
import engine.observer.InternalRenderable;
import engine.observer.Renderable;
import ide.layout.windows.ErrorWindow;

public abstract class RenderableApplication extends Application implements Renderable,InternalRenderable {
	public static long window;
	private boolean initialized;

	public static int windowWidth = 0;
	public static int windowHeight = 0;
	public static int screenPixelRatio = 1;

	public static int viewportWidth = 0;
	public static int viewportHeight = 0;
	
	public static double mouseX;
	public static double mouseY;
	
	public static double mouseDeltaX;
	public static double mouseDeltaY;
	public static boolean lockMouse;
	
	private boolean windowResizing;
	
	public static boolean GLFW_INITIALIZED;
	
	public static Pipeline pipeline;

	public void attachRenderable( Renderable o ) {
		renderThread.attach(o);
	}
	
	private boolean grabbed;
	@Override
	public void internalRender() {
		if ( GLFW.glfwWindowShouldClose(window) )
			return;
		
		if ( windowResizing )
			return;

		int[] windowWidthArr = {0}, windowHeightArr = {0};
		int[] frameBufferWidthArr = {0}, frameBufferHeightArr = {0};
		int[] xposArr = {0}, yposArr = {0};
		glfwGetWindowSize(window, windowWidthArr, windowHeightArr);
		glfwGetFramebufferSize(window, frameBufferWidthArr, frameBufferHeightArr);
		glfwGetWindowPos(window, xposArr, yposArr);

		windowWidth = windowWidthArr[0];
		windowHeight = windowHeightArr[0];
		if ( windowWidthArr[0] > 0 ) {
			screenPixelRatio = frameBufferWidthArr[0]/windowWidthArr[0];
		}

		viewportWidth = windowWidth*screenPixelRatio;
		viewportHeight = windowHeight*screenPixelRatio;
		
		double[] mxpos = {0}, mypos = {0};
		GLFW.glfwGetCursorPos(window, mxpos, mypos);
		double tx = mouseX;
		double ty = mouseY;
		mouseX = mxpos[0] - getMouseOffset().x;
		mouseY = mypos[0] - getMouseOffset().y;
		mouseDeltaX = mouseX-tx;
		mouseDeltaY = mouseY-ty;
		
		if ( shouldLockMouse() && lockMouse ) {
			if ( !grabbed ) {
				GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
				grabbed = true;
				
				double x = windowWidth/2f;
				double y = windowHeight/2f;
				GLFW.glfwSetCursorPos(window, x, y);
				mouseX = x - getMouseOffset().x;
				mouseY = y - getMouseOffset().y;
				mouseDeltaX = 0;
				mouseDeltaY = 0;
			}
		} else {
			if ( grabbed ) {
				GLFW.glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
				grabbed = false;
			}
		}
	}

	protected boolean shouldLockMouse() {
		return true;
	}
	
	protected abstract Vector2f getMouseOffset();

	@Override
	protected void onStart(String[] args) {
		ClientLauncher.launch(this);
		
		if ( !glfwInit() ) {
			GLFW_INITIALIZED = false;
			new ErrorWindow("Unable to initialize GLFW.", true);
			return;
		}
		
		GLFWErrorCallback.createPrint(System.err).set();

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE); // the window will be resizable

		// Core OpenGL version 3.2
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);

		// Create the window
		window = glfwCreateWindow(1024, 640, "Window", NULL, NULL);
		if ( window == NULL ) {
			new ErrorWindow("Failed to create the GLFW window.", true);
			throw new RuntimeException("Failed to create the GLFW window");
		}

		GLFW.glfwSetWindowSizeCallback(window, new GLFWWindowSizeCallbackI() {
			@Override
			public void invoke(long handle, int wid, int hei) {
				if ( !initialized ) {
					return;
				}
				windowResizing = true;
				windowWidth = wid;
				windowHeight = hei;
				renderThread.forceUpdate();
				windowResizing = false;
			}
		});

		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);
			windowWidth = pWidth.get(0);
			windowHeight = pHeight.get(0);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
					window,
					(vidmode.width() - pWidth.get(0)) / 2,
					(vidmode.height() - pHeight.get(0)) / 2
					);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);

		// Enable v-sync
		glfwSwapInterval(0);

		// Make the window visible
		glfwShowWindow(window);

		// Setup opengl
		GL.createCapabilities();

		// Start thread
		try {
			GLFW_INITIALIZED = true;
			internalInitialize();
			initialize(args);
			initialized = true;
			Resources.initialize();
		} catch(Exception e) {
			e.printStackTrace();
			new ErrorWindow("Error initializing engine.", true);
			return;
		}
		renderThread.run();
	}
}
