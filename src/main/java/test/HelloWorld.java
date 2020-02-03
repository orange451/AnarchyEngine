package test;

import org.lwjgl.glfw.GLFW;

import engine.ClientLocalRunner;
import engine.Game;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.light.PointLight;

public class HelloWorld extends ClientLocalRunner  {
	
	@Override
	public void loadScene(String[] args) {
		
		// Set ambient
		Game.lighting().setAmbient(Color3.newInstance(64, 64, 64));
		
		// Skybox
		DynamicSkybox sky = new DynamicSkybox();
		sky.setParent(Game.lighting());
		Game.lighting().setSkybox(sky);
		
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
		p.setName("Teapot");
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
			final float CAMERA_DIST = 2;
			final float CAMERA_PITCH = 0.25f;
			
			// Get turn direction
			int d = 0;
			if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_E) )
				d++;
			if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_Q) )
				d--;
			
			// Get the camera
			Camera camera = Game.workspace().getCurrentCamera();
			
			// Compute new rotation
			float yaw = camera.getYaw();
			yaw += d * delta;
			
			// Update the camera
			camera.orbit( Vector3.zero(), CAMERA_DIST, yaw, CAMERA_PITCH );
			
		});
	}
	
	public static void main(String[] args) {
		new HelloWorld();
	}
}
