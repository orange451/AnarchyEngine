package test;

import ide.RunnerClient;

public class RunnerTest extends RunnerClient {
	
	@Override
	public void loadScene(String[] args) {
		super.loadScene(new String[] {"Projects/Fruit/Fruit.json"});
	}

	public static void main(String[] args) {
		launch(args);
	}
}
