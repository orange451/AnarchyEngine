package test;

import engine.ClientRunner;
import engine.io.Load;

public class RunnerTest extends ClientRunner {

	@Override
	public void loadScene(String[] args) {
		Load.load("Projects/Fruit/Fruit.json");
	}

	public static void main(String[] args) {
		new RunnerTest();
	}
}
