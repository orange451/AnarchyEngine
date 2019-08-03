package test;

import engine.InternalRenderThread;
import engine.application.impl.ClientApplication;

public class RunnerTest extends ClientApplication {
	
	@Override
	public void loadScene(String[] args) {
		super.loadScene(new String[] {"Projects/Fruit/Fruit.json"});
		
		InternalRenderThread.desiredFPS = 200;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
