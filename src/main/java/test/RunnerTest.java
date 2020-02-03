package test;

import engine.ClientLocalRunner;
import engine.io.Load;

public class RunnerTest extends ClientLocalRunner {

	@Override
	public void loadScene(String[] args) {
		Load.load("Projects/Fruit/Fruit.json");
	}

	public static void main(String[] args) {
		new RunnerTest();
	}
}
