package engine.io;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

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
import engine.util.FileUtils;
import ide.ErrorWindow;
import ide.IDE;

public class Load {
	private static ArrayList<LoadedInstance> instances;
	
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
		// Load from JSON file
		boolean loadedJSON = false;
		JSONObject obj = null;
		try {
			JSONParser parser = new JSONParser();
			obj = (JSONObject) parser.parse(new FileReader(path));
			loadedJSON = true;
		} catch (Exception e) {
			e.printStackTrace();
			new ErrorWindow("There was a problem reading this file. 001");
		}
		
		// If JSON wasn't loaded, break free
		if ( !loadedJSON )
			return;
		
		// Set title
		Game.saveFile = path;
		Game.saveDirectory = new File(path).getParent();
		GLFW.glfwSetWindowTitle(IDE.window, IDE.TITLE + " [" + FileUtils.getFileDirectoryFromPath(path) + "]");
		
		// Unload the current game
		Game.unload();
		
		// Load the json
		parseJSON(obj);
	}
	
	public static void parseJSON(JSONObject obj) {
		// Read in the objects from JSON
		instances = new ArrayList<LoadedInstance>();
		readObjects(obj);
		
		try {
			// Load in services first
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				if ( inst.instance instanceof Service ) {
					loadObject(inst);
				}
			}
			
			// Correct instances (properties and such)
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				loadObject(inst);
			}
			
	
			// Force set parents
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				int parent = inst.Parent;

				if ( parent != -1 && inst.loaded ) {
					LoadedInstance p = getInstanceFromReference(parent);
					if ( !inst.instance.getParent().equals(p.instance) ) {
						inst.instance.forceSetParent(p.instance);
						//System.out.println("Setting parent of: " + inst.instance + "\tto\t" + p.instance);
					}
				}
			}
			
			// Tell game we're loaded
			Game.load();
		} catch(Exception e ) {
			e.printStackTrace();
			new ErrorWindow("There was a problem reading this file. 002");
		}
	}

	private static void loadObject(LoadedInstance inst) {
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
				value = getInstanceFromReference(pointer).instance;
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

	private static void readObjects(JSONObject obj) {
		LoadedInstance root = new LoadedInstance();
		root.ClassName = (String) obj.get("ClassName");
		root.Name = (String) obj.get("Name");
		root.Reference = loadReference("Reference",obj);
		root.Parent = loadReference("Parent",obj);
		
		if ( root.ClassName.equals("Game") ) {
			root.instance = (Instance) Game.game();
		} else {
			LuaValue temp = Game.game().get(root.ClassName);
			if ( temp.isnil() ) {
				root.instance = (Instance) Instance.instance(root.ClassName);
			} else {
				root.instance = (Instance) temp;
			}
		}
		
		if ( obj.get("Properties") != null ) {
			JSONObject properties = (JSONObject) obj.get("Properties");
			@SuppressWarnings("unchecked")
			String[] keys = (String[]) properties.keySet().toArray(new String[properties.keySet().size()]);
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i];
				Object t = properties.get(key);
				PropertyValue<?> v = PropertyValue.parse(t);
				root.properties.put(key, v);
			}
		}
		
		instances.add(root);
		
		JSONArray children = (JSONArray) obj.get("Children");
		for (int i = 0; i < children.size(); i++) {
			readObjects((JSONObject) children.get(i));
		}
	}
	
	private static int loadReference(String field, JSONObject obj) {
		JSONObject r = (JSONObject) obj.get(field);
		if ( r != null && r.get("Type").equals("Reference") ) {
			return Integer.parseInt(""+r.get("Value"));
		}
		return -1;
	}
	
	protected static LoadedInstance getInstanceFromReference(int ref) {
		for (int i = 0; i < instances.size(); i++) {
			if ( instances.get(i).Reference == ref ) {
				return instances.get(i);
			}
		}
		
		return null;
	}
	
	static class LoadedInstance {
		public boolean loaded;
		public String Name;
		public String ClassName;
		public int Reference;
		public int Parent;
		public Instance instance;
		
		public HashMap<String,PropertyValue<?>> properties = new HashMap<String,PropertyValue<?>>();
	}
	
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
					
					Class<? extends LuaValuetype> c = LuaValuetype.DATA_TYPES.get(type);
					if ( c != null ) {
						LuaValuetype o = null;
						try {
							Method method = c.getMethod("fromJSON", JSONObject.class);
							Object ot = method.invoke(null, temp);
							o = (LuaValuetype)ot;
						}catch( Exception e ) {
							e.printStackTrace();
						}
						
						return new PropertyValue<LuaValuetype>(o);
					}
				}
			}
			
			return null;
		}
	}
}
