# Overview

Anarchy Engine is a java-based game engine. It's designed to aid in the creation of video games primarily with the intent to be multiplayer. It uses OpenGL for rendering and lua for high-level scripting. Games can be made with the editor or in your favoriate java-IDE directly.

For support, see: http://manta.games/

# Required Libraries
- [LWJGL3](https://www.lwjgl.org/)
- [JOML](https://github.com/JOML-CI/JOML) (Can download with [LWJGL](https://www.lwjgl.org/customize))
- [NanoVG](https://github.com/memononen/nanovg) (Can download with [LWJGL](https://www.lwjgl.org/customize))
- [Assimp](https://github.com/assimp/assimp) (Can download with [LWJGL](https://www.lwjgl.org/customize))
- [STB](https://github.com/nothings/stb) (Can download with [LWJGL](https://www.lwjgl.org/customize))
- [LWJGUI](https://github.com/orange451/LWJGUI/)
- [LibGDX+Bullet](https://libgdx.badlogicgames.com/old-site/releases/)
- [Kryonet](https://github.com/EsotericSoftware/kryonet)
- [JSON-Simple](https://code.google.com/archive/p/json-simple/downloads)
- [luaj](https://github.com/luaj/luaj)

# Screenshots
![pong](https://i.imgur.com/EBIDL8M.gif)
![editor1](https://i.imgur.com/580uHdZ.png)
![pbr](https://cdn.discordapp.com/attachments/541818498293170177/568638439914733580/unknown.png)
![editor2](https://cdn.discordapp.com/attachments/511187289897173009/522436178088034305/unknown.png)

# Setup (Using Java directly)
1) Ensure the proper libraries are downloaded, and include them in your projects build path. Include the source to this project as well, or export it to a library.
2) Sample hello world:
```
package test;

import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import ide.RunnerClient;
import luaengine.RunnableArgs;
import luaengine.type.data.Color3;
import luaengine.type.data.Vector3;
import luaengine.type.object.insts.GameObject;
import luaengine.type.object.insts.Material;
import luaengine.type.object.insts.Mesh;
import luaengine.type.object.insts.PointLight;
import luaengine.type.object.insts.Prefab;

public class HelloWorld extends RunnerClient {
	
	@Override
	public void loadScene(String[] args) {
		
		// Set ambient
		Game.lighting().setAmbient(Color3.newInstance(64, 64, 64));
		
		// Make a sphere
		Mesh mesh = Game.assets().newMesh();
		mesh.sphere(1);
		
		// Base material
		Material material = Game.assets().newMaterial();
		material.setRoughness(0.1f);
		material.setMetalness(0.0f);
		material.setReflective(0.0f);
		material.setColor(Color3.white());
		
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
		
		// Camera controller
		Game.runService().renderSteppedEvent().connect( new RunnableArgs() {
			final int CAMERA_DIST = 2;
			double t = Math.PI/2f;
			
			@Override
			public void run(LuaValue[] args) {
				double delta = args[0].todouble();
				
				// Get direction value
				int d = 0;
				if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_E) )
					d++;
				if ( Game.userInputService().isKeyDown(GLFW.GLFW_KEY_Q) )
					d--;
				t += d*delta;
				
				// Rotate the camera based on the direction
				float xx = (float) (Math.cos(t) * CAMERA_DIST);
				float yy = (float) (Math.sin(t) * CAMERA_DIST);

				Game.workspace().getCurrentCamera().setPosition(Vector3.newInstance(xx,yy,CAMERA_DIST*0.75f));
				Game.workspace().getCurrentCamera().setLookAt(Vector3.newInstance(0, 0, 0));
			}
		});
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
```
