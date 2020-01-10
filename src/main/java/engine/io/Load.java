/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.io;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.Game;
import engine.lua.type.LuaValuetype;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.util.FileUtils;
import ide.IDE;
import ide.layout.windows.ErrorWindow;

public class Load {
	
	public static void load() {
		String path = "";
		MemoryStack.stackPush();
		PointerBuffer outPath = MemoryStack.stackMallocPointer(1);
		File f1 = new File("");
		File f2 = new File("Projects");
		int result = NativeFileDialog.NFD_OpenDialog("json", (f2 == null ? f1 : f2).getAbsolutePath(), outPath);
		if ( result == NativeFileDialog.NFD_OKAY ) {
			path = outPath.getStringUTF8(0);
		} else {
			return;
		}
		MemoryStack.stackPop();
		
		// Load the desired path
		load(path);
	}
	
	public static void load(String path) {
		load(path, true);
	}
	
	public static void load(String path, boolean reset) {
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
		if ( parseJSON(obj) == null )
			return;
		
		// Tell game we're loaded
		if ( reset )
			Game.load();
	}

	/**
	 * Desearializes a JSONObject(s) into Instances.
	 * @param obj
	 */
	public static Instance parseJSON(JSONObject obj) {
		return parseJSON( false, obj );
	}
	
	/**
	 * Desearializes a JSONObject(s) into Instances.
	 * @param removeUnusedInstances
	 * @param obj
	 * @return
	 */
	public static Instance parseJSON(boolean removeUnusedInstances, JSONObject obj) {
		
		// Read in the objects from JSON
		ArrayList<LoadedInstance> instances = new ArrayList<LoadedInstance>();
		HashMap<Long, LoadedInstance> instancesMap = new HashMap<>();
		HashMap<Long, Instance> unmodifiedInstances = null;
		readObjects(instances, instancesMap, obj);
		
		if ( removeUnusedInstances )
			unmodifiedInstances = Game.game().getInstanceMap();
		
		try {
			List<LoadedInstance> services = new ArrayList<LoadedInstance>();
			
			// Load in services first
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				if ( inst.instance instanceof Service ) {
					loadObject(instancesMap, inst);
					services.add(inst);
				}
			}
			
			// Correct instances (properties and such)
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				loadObject(instancesMap, inst);
			}
			
			// Force set parents (of non services)
			for (int i = 0; i < instances.size(); i++) {
				LoadedInstance inst = instances.get(i);
				long parent = inst.Parent;
				
				// Remove reference to unused
				if ( removeUnusedInstances )
					unmodifiedInstances.remove(inst.instance.getSID());

				if ( parent != -1 && inst.loaded ) {
					Instance p = getInstanceFromReference(instancesMap, parent);
					if ( !inst.instance.equals(p) ) {
						inst.instance.forceSetParent(p);
						//System.out.println("Setting parent of: " + inst.instance + "\tto\t" + p);
					}
				}
			}
			
			// Parent services
			for (int i = 0; i < services.size(); i++) {
				LoadedInstance inst = services.get(i);
				inst.instance.forceSetParent(Game.game());
			}
			
			// Delete unused instances
			if ( removeUnusedInstances ) {
				Set<Entry<Long, Instance>> insts = unmodifiedInstances.entrySet();
				Iterator<Entry<Long, Instance>> iterator = insts.iterator();
				while ( iterator.hasNext() ) {
					Entry<Long, Instance> entry = iterator.next();
					Instance t = entry.getValue();
					if ( !t.isDestroyed() ) {
						t.destroy();
					}
				}
			}
			
			return instances.get(0).instance;
		} catch(Exception e ) {
			e.printStackTrace();
			new ErrorWindow("There was a problem reading this file. 002");
		}
		
		return null;
	}

	private static void loadObject(HashMap<Long, LoadedInstance> instancesMap, LoadedInstance inst) {
		if ( inst.loaded )
			return;
		
		if ( !inst.instance.getName().equals(inst.Name) )
			inst.instance.forceSetName(inst.Name);
		inst.loaded = true;
		
		// Set all properties
		HashMap<String, PropertyValue<?>> properties = inst.properties;
		Iterator<Entry<String, PropertyValue<?>>> entrySet = properties.entrySet().iterator();
		while(entrySet.hasNext()) {
			Entry<String, PropertyValue<?>> set = entrySet.next();
			String key = set.getKey();
			PropertyValue<?> val = set.getValue();
			
			// Dont load SID's if game is paused or if non existant
			if ( key.equals("SID") ) {
				if (!Game.isRunning())
					continue;
				
				if ( Long.parseLong(val.value.toString()) == -1 )
					continue;
			}
			
			// Make sure user isn't trying to inject new fields!
			boolean hasField = inst.instance.containsField(LuaValue.valueOf(key));
			if ( !hasField )
				continue;
			
			// Get value for property
			Object value = val.getValue();
			if ( val.pointer ) {
				long pointer = ((Long) value).longValue();
				value = getInstanceFromReference(instancesMap, pointer);
			}
			
			// If value is set, set it in ECS
			if ( value != null ) {
				LuaValue lv = (value instanceof LuaValue)?(LuaValue)value:CoerceJavaToLua.coerce(value);
				if ( !inst.instance.get(key).equals(lv) ) {
					inst.instance.rawset(key, lv);
					try{ inst.instance.set(key, lv); } catch(Exception e) {};
				}
			}
		}
	}

	private static void readObjects(ArrayList<LoadedInstance> instances, HashMap<Long, LoadedInstance> instancesMap, JSONObject obj) {
		LoadedInstance root = new LoadedInstance();
		root.ClassName = (String) obj.get("ClassName");
		root.Name = (String) obj.get("Name");
		root.Reference = loadReference("Reference",obj);
		root.Parent = loadReference("Parent",obj);
		
		if ( root.ClassName.equals("Game") ) {
			root.instance = Game.game();
		} else {
			LuaValue temp = Game.game().get(root.ClassName);
			if ( temp.isnil() ) {
				Object SIDRef = ((JSONObject)obj.get("Properties")).get("SID");
				if ( SIDRef != null ) {
					Instance inGame = Game.getInstanceFromSID(Long.parseLong(SIDRef.toString()));
					if ( inGame != null && !inGame.isDestroyed() && inGame.getClassName().eq_b(LuaValue.valueOf(root.ClassName)) ) {
						root.instance = inGame;
					} else {
						root.instance = (Instance) Instance.instance(root.ClassName);
					}
				} else {
					root.instance = (Instance) Instance.instance(root.ClassName);
				}
			} else {
				root.instance = (Instance) temp;
			}
		}
		
		if ( obj.get("Properties") != null ) {
			JSONObject properties = (JSONObject) obj.get("Properties");
			for (Object entry : properties.entrySet()) {
				@SuppressWarnings("unchecked")
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
			readObjects( instances, instancesMap, (JSONObject) children.get(i));
		}
	}
	
	private static long loadReference(String field, JSONObject obj) {
		JSONObject r = (JSONObject) obj.get(field);
		if ( r != null && r.get("Type").equals("Reference") ) {
			return Long.parseLong(r.get("Value").toString());
		}
		return -1;
	}
	
	protected static Instance getInstanceFromReference(HashMap<Long, LoadedInstance> instancesMap, long ref) {
		LoadedInstance loaded = instancesMap.get(ref);
		if ( loaded != null )
			return loaded.instance;
		
		// Now search for instance by SID if it wasn't found before.
		return Game.getInstanceFromSID(ref);
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

		public static PropertyValue<?> parse(Object fieldKey) {
			if ( fieldKey == null ) {
				return new PropertyValue<LuaValue>(LuaValue.NIL);
			}
			if ( fieldKey instanceof Boolean ) {
				return new PropertyValue<Boolean>((Boolean)fieldKey);
			}
			if ( fieldKey instanceof Double ) {
				return new PropertyValue<Double>((Double)fieldKey);
			}
			if ( fieldKey instanceof Float ) {
				return new PropertyValue<Float>((Float)fieldKey);
			}
			if ( fieldKey instanceof String ) {
				return new PropertyValue<String>((String)fieldKey);
			}
			if ( fieldKey instanceof JSONObject ) {
				JSONObject j = (JSONObject)fieldKey;
				
				if ( j.get("Type").equals("Reference") ) {
					long v = Long.parseLong(j.get("Value").toString());
					
					// Make sure the pointed to hash object matches exactly!
					Object hashEntry = j.get("Hash");
					if ( hashEntry != null ) {
						Instance temp = Game.getInstanceFromSID(v);
						if ( temp != null ) {
							if ( !temp.hashFields().equals(hashEntry) )
								v = -1;
						}
					}
					
					return new PropertyValue<Long>(v, true);
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
