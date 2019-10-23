package engine.lua;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import engine.lua.type.object.Instance;
import engine.lua.type.object.ScriptBase;
import engine.util.ClassFinder;

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
		
		// Preload object types. They run code to attach themselves to the lua engine.
		{
			// load Data types (color/vector/ect)
			loadDataTypes("engine.lua.type.data");
			
			// Register instance types (GameObject/Camera/Etc)
			loadDataTypes("engine.lua.type.object.insts");
		}
	}
	
	/**
	 * Every object inside engine.lua.type.objects.insts needs to be instantiated ONCE at run-time.
	 */
	private static void loadDataTypes(String packageName) {
		// Get classes in subpackage
		ArrayList<Class<?>> cls = ClassFinder.getClassesFromPackage(packageName);
		Class<?>[] claz = cls.toArray(new Class[cls.size()]);
		
		// Load them in
		System.out.println("Loading classes for: " + claz + " ("+claz.length+")");
		for (int i = 0; i < claz.length; i++) {
			Class<?> c = claz[i];
			try {
				if ( !c.toString().contains(packageName) ) {
					System.out.println("Throwing out: " + c);
					continue;
				}
				
				if ( c.toString().contains("$") )
					continue;
				
				System.out.println("\tLoaded: " + c);
				LuaDatatype v = (LuaDatatype) c.getDeclaredConstructor().newInstance();
				if ( v instanceof Instance ) {
					((Instance)v).destroy();
				} else {
					v.cleanup();
				}
			} catch (Exception e) {
				System.out.println("\t\tError");
			}
		}
	}
	
	public static ScriptRunner runLua(String source) {
		return runLua(source, null);
	}

	public static ScriptRunner runLua(String source, ScriptBase owner) {
		String name = "CMD";
		String full = name;
		if ( owner != null ) {
			System.out.println("Running: " + owner.getFullName());
			name = owner.getName();
			full = owner.getFullName();
		}
		
		try{
			LuaValue currentChunk = LuaEngine.globals.load(source, name);
			return (ScriptRunner) globals.get("spawn").call(currentChunk, owner); // Run spawn class from GameEngineLib
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