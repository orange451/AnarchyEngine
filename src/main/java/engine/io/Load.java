package engine.io;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.Game;
import engine.lua.type.LuaValuetype;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.services.GameLua;
import engine.util.FileUtils;
import ide.IDE;
import ide.layout.windows.ErrorWindow;

public class Load {
	private static ArrayList<LoadedInstance> instances;
	private static HashMap<Long, LoadedInstance> instancesMap;
	
	public static void load() {
		String path = "";
		PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
		File f1 = new File("");
		File f2 = new File("Projects");
		int result = NativeFileDialog.NFD_OpenDialog("json", (f2 == null ? f1 : f2).getAbsolutePath(), outPath);
		if ( result == NativeFileDialog.NFD_OKAY ) {
			path = outPath.getStringUTF8(0);
		} else {
			return;
		}
		
		// Load the desired path
		load(path);
	}
	
	public static void load(String path) {
		load(Game.game(), path, true);
	}
	
	public static void load(GameLua game, String path, boolean reset) {
		// Load from JSON file
		boolean loadedJSON = false;
		JSONObject obj = null;
		try {
			JSONParser parser = new JSONParser();
			obj = (JSONObject) parser.parse(new FileReader(path));
			loadedJSON = true;
		} catch (Exception e) {
			//e.printStackTrace();
			new ErrorWindow("There was a problem reading this file. 001");
		}
		
		// If JSON wasn't loaded, break free
		if ( !loadedJSON )
			return;
		
		// Set title
		if ( reset ) {
			Game.saveFile = path;
			Game.saveDirectory = new File(path).getParent();
			GLFW.glfwSetWindowTitle(IDE.window, IDE.TITLE + " [" + FileUtils.getFileDirectoryFromPath(path) + "]");
		}
		
		// Unload the current game
		if ( reset )
			Game.unload();
		
		// Load the json
		if ( !parseJSON( game, obj) )
			return;
		
		// Tell game we're loaded
		if ( reset )
			Game.load();
	}
	
	/**
	 * Desearializes a JSONObject into Instances.
	 * @param obj
	 */
	public static boolean parseJSON(GameLua game, JSONObject... obj) {
		// Read in the objects from JSON
		instances = new ArrayList<LoadedInstance>();
		instancesMap = new HashMap<>();
		for ( int i = 0; i < obj.length; i++) {
			readObjects( game, obj[i]);
		}
		
		try {
			List<LoadedInstance> services = new ArrayList<LoadedInstance>();
			
			// Load in services first
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				if ( inst.instance instanceof Service ) {
					loadObject(game, inst);
					services.add(inst);
				}
			}
			
			// Correct instances (properties and such)
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				loadObject(game, inst);
			}
			
	
			// Force set parents (of non services)
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				long parent = inst.Parent;

				if ( parent != -1 && inst.loaded ) {
					Instance p = getInstanceFromReference(game, parent);
					//if ( !inst.instance.getParent().equals(p) ) {
						inst.instance.forceSetParent(p);
						//System.out.println("Setting parent of: " + inst.instance + "\tto\t" + p);
					//}
				}
			}
			
			// Parent services
			for (int i = 0; i < services.size(); i++) {
				LoadedInstance inst = services.get(i);
				inst.instance.forceSetParent(game);
			}
			
			return true;
		} catch(Exception e ) {
			e.printStackTrace();
			new ErrorWindow("There was a problem reading this file. 002");
		}
		
		return false;
	}

	private static void loadObject(GameLua game, LoadedInstance inst) {
		if ( inst.loaded )
			return;
		
		if ( !inst.instance.getName().equals(inst.Name) )
			inst.instance.forceSetName(inst.Name);
		inst.loaded = true;
		
		// Set all properties
		HashMap<String, PropertyValue<?>> properties = inst.properties;
		String[] propertyKeys = properties.keySet().toArray(new String[properties.keySet().size()]);
		for (int j = 0; j < propertyKeys.length; j++) {
			String key = propertyKeys[j];
			if ( key.equals("SID") && !Game.isRunning() )
				continue;
			
			PropertyValue<?> p = properties.get(key);
			Object value = p.getValue();
			if ( p.pointer ) {
				int pointer = ((Integer) value).intValue();
				value = getInstanceFromReference(game, pointer);
			}
			
			if ( value != null ) {
				LuaValue lv = (value instanceof LuaValue)?(LuaValue)value:CoerceJavaToLua.coerce(value);
				if ( !inst.instance.get(key).equals(lv) ) {
					inst.instance.rawset(key, lv);
					try{ inst.instance.set(key, lv); } catch(Exception e) {};
				}
			}
		}
	}

	private static void readObjects(GameLua game, JSONObject obj) {
		LoadedInstance root = new LoadedInstance();
		root.ClassName = (String) obj.get("ClassName");
		root.Name = (String) obj.get("Name");
		root.Reference = loadReference("Reference",obj);
		root.Parent = loadReference("Parent",obj);
		
		if ( root.ClassName.equals("Game") ) {
			root.instance = game;
		} else {
			LuaValue temp = game.get(root.ClassName);
			if ( temp.isnil() ) {
				root.instance = (Instance) Instance.instance(root.ClassName);
			} else {
				root.instance = (Instance) temp;
			}
		}
		
		if ( obj.get("Properties") != null ) {
			JSONObject properties = (JSONObject) obj.get("Properties");
			for (Object entry : properties.entrySet()) {
				Map.Entry<Object,Object> entry2 = (Entry<Object, Object>) entry;
				String key = entry2.getKey().toString();
				Object t = entry2.getValue();
				PropertyValue<?> v = PropertyValue.parse(t);
				root.properties.put(key, v);
			}
		}
		
		instances.add(root);
		instancesMap.put(root.Reference, root);
		
		JSONArray children = (JSONArray) obj.get("Children");
		for (int i = 0; i < children.size(); i++) {
			readObjects(game, (JSONObject) children.get(i));
		}
	}
	
	private static long loadReference(String field, JSONObject obj) {
		JSONObject r = (JSONObject) obj.get(field);
		if ( r != null && r.get("Type").equals("Reference") ) {
			return Long.parseLong(""+r.get("Value"));
		}
		return -1;
	}
	
	protected static Instance getInstanceFromReference(GameLua game, long ref) {
		LoadedInstance loaded = instancesMap.get(ref);
		if ( loaded != null )
			return loaded.instance;
		
		// Now search for instance by SID if it wasn't found before.
		return Game.getInstanceFromSID(game, ref);
	}
	
	static class LoadedInstance {
		public boolean loaded;
		public String Name;
		public String ClassName;
		public long Reference;
		public long Parent;
		public Instance instance;
		
		public HashMap<String,PropertyValue<?>> properties = new HashMap<String,PropertyValue<?>>();
	}
	
	private static HashMap<String, Method> dataTypeToMethodMap = new HashMap<String, Method>();
	
	static class PropertyValue<T> {
		private T value;
		public boolean pointer;
		
		public PropertyValue(T t) {
			this(t, false);
		}
		
		public PropertyValue(T t, boolean pointer) {
			this.value = t;
			this.pointer = pointer;
		}

		public T getValue() {
			return value;
		}

		public static PropertyValue<?> parse(Object t) {
			if ( t == null ) {
				return new PropertyValue<LuaValue>(LuaValue.NIL);
			}
			if ( t instanceof Boolean ) {
				return new PropertyValue<Boolean>((Boolean)t);
			}
			if ( t instanceof Double ) {
				return new PropertyValue<Double>((Double)t);
			}
			if ( t instanceof Float ) {
				return new PropertyValue<Float>((Float)t);
			}
			if ( t instanceof String ) {
				return new PropertyValue<String>((String)t);
			}
			if ( t instanceof JSONObject ) {
				JSONObject j = (JSONObject)t;
				
				if ( j.get("Type").equals("Reference") ) {
					int v = Integer.parseInt(""+j.get("Value"));
					return new PropertyValue<Integer>(v, true);
				}
				
				if ( j.get("Type").equals("Datatype") ) {
					JSONObject data = (JSONObject) j.get("Value");
					String type = (String) data.get("ClassName");
					JSONObject temp = (JSONObject) data.get("Data");
					
					// Get the fromJSON method
					Method method = dataTypeToMethodMap.get(type);
					if ( method == null ) {
						Class<? extends LuaValuetype> c = LuaValuetype.DATA_TYPES.get(type);
						try {
							method = c.getMethod("fromJSON", JSONObject.class);
							dataTypeToMethodMap.put(type, method);
						} catch (NoSuchMethodException e) {
							//
						}
					}
					
					// Call it to get a returned value
					try {
						Object ot = method.invoke(null, temp);
						LuaValuetype o = (LuaValuetype)ot;
						return new PropertyValue<LuaValuetype>(o);
					}catch( Exception e ) {
						e.printStackTrace();
					}
				}
			}
			
			return null;
		}
	}
}
