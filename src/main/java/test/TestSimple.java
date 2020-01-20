package test;

import org.lwjgl.glfw.GLFW;

import engine.ClientRunner;
import engine.Game;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Material;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.PointLight;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.Texture;

public class TestSimple extends ClientRunner {
	
	@Override
	public void loadScene(String[] args) {
		
		// Set ambient
		Game.lighting().setAmbient(Color3.newInstance(32,32,32));
		
		// Make a sphere
		Mesh mesh = Game.assets().newMesh();
		mesh.sphere(1);
		
		// MATERIAL PATH
		String materialPath = "Resources/Testing/PBR/iron/";
		
		// Textures
		Texture texture1 = Game.assets().newTexture();
		texture1.setFilePath( materialPath + "albedo.png");
		texture1.setSRGB(true); // Albedo texture needs this to look correct
		
		Texture texture2 = Game.assets().newTexture();
		texture2.setFilePath( materialPath + "normal.png");
		
		Texture texture3 = Game.assets().newTexture();
		texture3.setFilePath( materialPath + "roughness.png");
		
		Texture texture4 = Game.assets().newTexture();
		texture4.setFilePath( materialPath + "metallic.png");
		
		// Base material
		Material material = Game.assets().newMaterial();
		material.setDiffuseMap(texture1);
		material.setNormalMap(texture2);
		material.setRoughMap(texture3);
		material.setMetalMap(texture4);
		material.setRoughness(1.0f);
		material.setMetalness(1.0f);
		material.setReflective(0.1f);
		
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
		
		// Mark the camera as scriptable (no built-in camera controls)
		Game.workspace().getCurrentCamera().setCameraType("Scriptable");
		
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
		new TestSimple();
	}
}
