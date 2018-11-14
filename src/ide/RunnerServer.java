package ide;

import java.io.IOException;

import engine.InternalRenderThread;
import engine.application.Runner;
import engine.io.Load;
import lwjgui.Color;
import lwjgui.LWJGUI;
import lwjgui.scene.control.Label;

public class RunnerServer extends Runner {
	private Label label;
	
	public RunnerServer() {
		// Create FPS label
		label = new Label();
		label.setTextFill(Color.white);
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
}