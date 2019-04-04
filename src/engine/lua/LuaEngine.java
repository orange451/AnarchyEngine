package engine.lua;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import engine.lua.lib.Enums;
import engine.lua.lib.GameEngineLib;
import engine.lua.lib.PreventInfiniteInstructions;
import engine.lua.type.*;
import engine.lua.type.data.*;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.insts.*;

public class LuaEngine {
	public static Globals globals;

	public static void initialize() {
		if ( globals != null )
			return;

		// Normal Lua defaults
		globals = new Globals();
		globals.load(new JseBaseLib());
		globals.load(new PackageLib());
		globals.load(new Bit32Lib());
		globals.load(new TableLib());
		globals.load(new StringLib());
		globals.load(new CoroutineLib());
		globals.load(new JseMathLib());
		globals.load(new JseIoLib());
		globals.load(new JseOsLib());
		globals.load(new PreventInfiniteInstructions());
		globals.load(new GameEngineLib());
		globals.load(new Enums());
		LoadState.install(globals);
		LuaC.install(globals);
		
		
		// Load in all the datatypes/instances
		loadDataTypesTEMP();
		
		// MUST BE FIXED. ABOVE IS TEMP
		{
			// load Data types (color/vector/ect)
			//loadDataTypes("luaengine.type.data");
			
			// Register instance types
			//loadDataTypes("luaengine.type.object.insts");
		}
	}
	
	/**
	 * Every object inside engine.lua.type.objects.insts needs to be instantiated ONCE at run-time.
	 * <br>
	 * Ideally we should have a function that finds all these objects and creates them for us...
	 */
	private static void loadDataTypesTEMP() {
		
		// Vars
		new Color3();
		new Matrix4();
		new Vector2();
		new Vector3();
		
		// Objects
		new AssetFolder();
		new Camera();
		new Connection();
		new Folder();
		new GameObject();
		new Material();
		new Mesh();
		new Model();
		new PhysicsObject();
		new Player();
		new PlayerPhysics();
		new Prefab();
		new Script();
		new LocalScript();
		new GlobalScript();
		new Texture();
		new PlayerScripts();
		new PointLight();
		new Skybox();
		
		// Animation objects
		new AnimationData();
		new Animation();
		new AnimationKeyframe();
		new AnimationKeyframeSequence();
		new Animations();
		new Bone();
		new Bones();
		new BoneTree();
		new BoneTreeNode();
		new BoneWeight();
	}
	
	/*private static void loadDataTypes(String packageName) {
		// Get classes in subpackage
		Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage(packageName))
				.setScanners(new ResourcesScanner()));
		Set<Class<? extends Object>> subTypes = reflections.getSubTypesOf(Object.class);
		Class<?>[] claz = subTypes.toArray(new Class[subTypes.size()]);
		
		// Load them in
		System.out.println("Loading classes for: " + claz + " ("+claz.length+")");
		for (int i = 0; i < claz.length; i++) {
			Class<?> c = claz[i];
			try {
				if ( !c.toString().contains(packageName) ) {
					System.out.println("Throwing out: " + c);
					continue;
				}
				
				System.out.println("\tLoaded: " + c);
				LuaDatatype v = (LuaDatatype) c.getDeclaredConstructor().newInstance();
				if ( v instanceof Instance ) {
					((Instance)v).destroy();
				} else {
					v.cleanup();
				}
			} catch (Exception e) {
				System.out.println("\t\tError");
				//System.err.println("\t\t" + c);
				//e.printStackTrace();
			}
		}
	}*/
	
	public static ScriptData runLua(String source) {
		return runLua(source, null);
	}

	public static ScriptData runLua(String source, ScriptBase script) {
		String name = "CMD";
		String full = name;
		if ( script != null ) {
			System.out.println("Running: " + script.getFullName());
			name = script.getName();
			full = script.getFullName();
		}
		
		try{
			LuaValue currentChunk = LuaEngine.globals.load(source, name);
			return (ScriptData) globals.get("spawn").call(currentChunk, script); // Run spawn class from GameEngineLib
		}catch(LuaError e) {
			// This will only run for syntax errors. Not logical errors.
			String message = e.getMessage();
			String[] split = message.split(":");
			String reason = split[split.length-1];
			String line = split[split.length-2];

			LuaEngine.error("[" + full + "], Line " + line + ": " + reason);
		}
		
		return null;
	}

	public static void error(String warning) {
		String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
		String newMessage = timeStamp + " - " + warning + "\n";
		try {
			LuaEngine.globals.STDERR.write(newMessage.getBytes());
		} catch (IOException e1) {
			//
		}
	}

	public static void parseError(LuaError e, String fullName) {
		String message = e.getMessage().split("\n")[0];
		String[] split = message.split(":");
		String reasonTemp = split[split.length-1];
		String[] split2 = reasonTemp.split(" ");
		String line = split2[0];
		String reason = reasonTemp.substring(line.length());

		LuaEngine.error("[" + fullName + "], Line " + line + ": " + reason);
	}

	public static void print(String str) {
		synchronized(LuaEngine.globals.STDOUT) {
			try {
				LuaEngine.globals.STDOUT.write(str.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}