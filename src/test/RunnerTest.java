package test;

import engine.Game;
import ide.RunnerClient;
import luaengine.type.object.insts.PointLight;

public class RunnerTest extends RunnerClient {
	
	@Override
	public void loadScene(String[] args) {
		super.loadScene(new String[] {"Projects/Fruit/Fruit.json"});
		
		PointLight pl = new PointLight();
		pl.setRadius(32);
		pl.setIntensity(10);
		pl.setPosition(0, 0, 8);
		pl.setParent(Game.workspace());
	}

	public static void main(String[] args) {
		launch(args);
	}
}
