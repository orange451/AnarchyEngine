package test;

import engine.application.impl.ClientApplication;

public class RunnerTest extends ClientApplication {
	
	@Override
	public void loadScene(String[] args) {
		super.loadScene(new String[] {"Projects/Fruit/Fruit.json"});
	}

	public static void main(String[] args) {
		launch(args);
	}
}
