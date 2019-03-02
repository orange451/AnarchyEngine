package engine.application.impl;

import java.io.IOException;

import org.joml.Vector2f;

import engine.InternalRenderThread;
import engine.application.Runner;
import engine.io.Load;
import lwjgui.LWJGUI;
import lwjgui.paint.Color;
import lwjgui.scene.control.Label;

public class ServerApplication extends Runner {
	private Label label;
	
	public ServerApplication() {
		// Create FPS label
		label = new Label();
		label.setTextFill(Color.WHITE);
		LWJGUI.runLater(()->{
			getRootPane().getChildren().add(label);
		});
	}
	
	@Override
	public void loadScene(String[] args) {
		
		// Load the desired scene file
		if ( args.length == 1 ) {
			Load.load(args[0]);
		} else {
			//Game.load();
		}
	}

	@Override
	public void render() {
		// Update UI
		label.setText("fps: "+InternalRenderThread.fps);
		
		// Render super (updates the pipeline & draws UI)
		super.render();
	}
	
	public static void main(String[] args) throws IOException {
		launch(args);
	}

	@Override
	protected Vector2f getMouseOffset() {
		return new Vector2f();
	}
}
