package engine.application;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.gl.Pipeline;
import engine.lua.type.object.services.UserInputService;
import lwjgui.LWJGUI;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.scene.Window;
import lwjgui.scene.layout.StackPane;

public abstract class Runner extends RenderableApplication {
	private static StackPane rootPane;
	
	public StackPane getRootPane() {
		return rootPane;
	}
	
	@Override
	public void initialize(String[] args) {
		// Add rendering pipeline
		pipeline = new Pipeline();
		
		// Enable LWJGUI on this window (used for UI drawing)
		Window win = LWJGUI.initialize(window);
		win.setWindowAutoDraw(false); // To make it so we control swapbuffers
		win.setWindowAutoClear(false); // To make it so we control glClear()
		
		// TEST UI
		rootPane = new StackPane();
		rootPane.setPadding(new Insets(4,4,4,4));
		rootPane.setAlignment(Pos.TOP_LEFT);
		rootPane.setBackground(null);
		win.getScene().setRoot(rootPane);
		
		// Pass user input to the user input service
		rootPane.setOnKeyPressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onKeyPressed(event.getKey());
		});
		rootPane.setOnKeyReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onKeyReleased(event.getKey());
		});
		rootPane.setMousePressedEvent(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMousePress(event.button);
		});
		rootPane.setMouseReleasedEvent(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMouseRelease(event.button);
		});
		
		// Load the scene
		loadScene(args);
		
		// Tell the game to run
		InternalGameThread.runLater(()->{
			Game.setRunning(true);
			
			// On first render, throw out an update.
			InternalRenderThread.runLater(()->{
				Game.getGame().gameUpdate(true);
			});
		});
	}

	@Override
	public void render() {
		if ( GLFW.glfwWindowShouldClose(window) )
			return;
		
		// Render pipeline
		pipeline.setSize(windowWidth, windowHeight);
		pipeline.render();
		
		// Set viewport
		GL11.glViewport(0, 0, viewportWidth, viewportHeight);

		// Draw pipeline's buffer to screen
		pipeline.ortho(); // Setup 2d drawing
		pipeline.shader_reset(); // Set shader
		pipeline.getPipelineBuffer().getTextureDiffuse().bind(); // Bind buffer
		pipeline.fullscreenQuad(); // draw it to screen

		LWJGUI.render(); // Gets directly rendered on-top of buffer (in same FBO)
	}
	
	public abstract void loadScene(String[] args);

	@Override
	public void tick() {
		//
	}
}
