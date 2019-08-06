package test;

import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import engine.application.impl.ClientApplication;
import engine.lua.RunnableArgs;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.PointLight;
import engine.lua.type.object.insts.Prefab;

public class HelloWorld extends ClientApplication {
	
	@Override
	public void loadScene(String[] args) {
		
		// Set ambient
		Game.lighting().setAmbient(Color3.newInstance(64, 64, 64));
		
		// Make a sphere
		Mesh mesh = Game.assets().newMesh();
		mesh.teapot(1);
		
		// Base material
		Material material = Game.assets().newMaterial();
		material.setRoughness(0.3f);
		material.setMetalness(0.1f);
		material.setReflective(0.1f);
		material.setColor(Color3.red());
		
		// Create prefab
		Prefab p = Game.assets().newPrefab();
		p.setName("Prefab0");
		p.addModel(mesh, material);
		
		// Create game object in the world with prefab
		GameObject obj = new GameObject();
		obj.setPrefab(p);
		obj.setParent(Game.workspace());
		
		// Add lights
		{
			int close = 8;
			int r = 48;
			int b = 10;
			int xx = 8;
			PointLight l1 = new PointLight();
			l1.setPosition(-xx, close, xx);
			l1.setRadius(r);
			l1.setIntensity(b);
			l1.setParent(Game.workspace());
			
			PointLight l2 = new PointLight();
			l2.setPosition(xx, close, xx);
			l2.setRadius(r);
			l2.setIntensity(b);
			l2.setParent(Game.workspace());
			
			PointLight l3 = new PointLight();
			l3.setPosition(-xx, close, -xx);
			l3.setRadius(r);
			l3.setIntensity(b);
			l3.setParent(Game.workspace());
			
			PointLight l4 = new PointLight();
			l4.setPosition(xx, close, -xx);
			l4.setRadius(r);
			l4.setIntensity(b);
			l4.setParent(Game.workspace());
			
			PointLight l5 = new PointLight();
			l5.setPosition(xx, -close*2, -xx);
			l5.setRadius(r);
			l5.setIntensity(b/2);
			l5.setParent(Game.workspace());
		}
		
		// Camera controller new
		Game.runService().renderSteppedEvent().connect( (params) -> {
			double delta = params[0].todouble();
			final int CAMERA_DIST = 2;
			
			// Get turn direction
			int d = 0;
			if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_E) )
				d++;
			if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_Q) )
				d--;
			
			// Get the camera
			Camera camera = Game.workspace().getCurrentCamera();
			float yaw = camera.getYaw();
			
			// Rotate camera
			yaw += (d) * delta;
			camera.orbit( Vector3.zero(), CAMERA_DIST, yaw, 0.25f );
			
		});
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
