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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.type.LuaValuetype;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Service;
import engine.lua.type.object.insts.Scene;
import engine.lua.type.object.insts.SceneInternal;
import engine.util.FileUtils;
import ide.layout.windows.ErrorWindow;
import lwjgui.scene.WindowManager;

public class Load {
	
	/**
	 * Load project without losing any changes. POTENTIALLY OPENS FILE WINDOW -- THREAD SAFE!
	 */
	public static void loadSafe() {
		Runnable r = () -> {
			WindowManager.runLater(() -> {
				Load.load();
			});
		};
		
		if ( Game.changes ) {
			Save.requestSave(r);
		} else {
			r.run();
		}
	}
	
	/**
	 * Force load project. OPENS FILE WINDOW -- NOT THREAD SAFE.
	 */
	public static void load() {
		String path = "";
		MemoryStack stack = MemoryStack.stackPush();
		PointerBuffer outPath = stack.mallocPointer(1);
		File f1 = new File("");
		File f2 = new File("Projects");
		int result = NativeFileDialog.NFD_OpenDialog("json", (f2 == null ? f1 : f2).getAbsolutePath(), outPath);
		if ( result == NativeFileDialog.NFD_OKAY ) {
			path = outPath.getStringUTF8(0);
		} else {
			return;
		}
		stack.pop();
		
		final String finalPath = path;
		
		// Load the desired path
		InternalGameThread.runLater(()->{
			load(finalPath);
		});
	}
	
	/**
	 * Force load a specified path. Will unload currently loaded resources
	 * @param path
	 */
	public static void load(String path) {
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
		Game.saveFile = path;
		Game.saveDirectory = new File(path).getParent();
		//GLFW.glfwSetWindowTitle(IDE.window, IDE.TITLE + " [" + FileUtils.getFileDirectoryFromPath(path) + "]");
		
		// Unload the current game
		Game.unload();
		
		// Load the json
		if ( obj.containsKey("Version") ) {
			System.out.println("USING NEW LOAD SYSTEM");
			if ( (Double)obj.get("Version") < 2 ) {
				System.out.println("USEING REALLY BAD LOAD FORMAT... Please update!");
				LoadDeprecated.load(path);
				return;
			} else {
				parseJSONInto((JSONObject)obj.get("ProjectData"), Game.project());
				
				// Force load starting scene...
				Game.game().loadScene(Game.project().scenes().getStartingScene());
			}
		} else {
			System.out.println("USING OLD LOAD SYSTEM");
			LoadDeprecated.load(path);
			return;
		}
		
		loadExternalObjects(path);
		
		// Tell game we're loaded
		Game.load();
		
		// Dont let the user CTRL Z after loading project.
		Game.historyService().getHistoryStack().clear();
	}
	
	public static void loadExternalObjects(String path) {
		// Load external stuff
		File scripts = new File(new File(path).getParent() + File.separator + "Resources" + File.separator + "Scripts");
		if ( scripts.exists() ) {
			try (Stream<Path> walk = Files.walk(Paths.get(scripts.getAbsolutePath()))) {
				List<Path> result = walk.filter(Files::isRegularFile).map(x -> x).collect(Collectors.toList());
				
				for (int i = 0; i < result.size(); i++) {
					File f = result.get(i).toFile();
					// Get uuid
					String uuidString = FileUtils.getFileNameWithoutExtension(FileUtils.getFileNameFromPath(f.getAbsolutePath()));
					UUID uuid = UUID.fromString(uuidString);
					System.out.println("Attempting to read in source data for instance: " + uuidString);
					
					// Read in source
					JSONParser parser = new JSONParser();
					JSONObject externalJSON = (JSONObject) parser.parse(new FileReader(f));
					String source = externalJSON.get("Source").toString();
					
					// Make sure proper instance exists
					Instance inst = Game.getInstanceFromUUID(uuid);
					System.out.println(inst);
					if ( inst != null && inst.containsField(LuaValue.valueOf("Source")) && inst.getField(LuaValue.valueOf("Source")).getType() == LuaString.class ) {
						System.out.println(source);
						
						// Set the source!
						inst.set(LuaValue.valueOf("Source"), source);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Attempt to load a scene from file (into Game)
	 * @param scene
	 * @return
	 */
	public static SceneInternal loadScene(Scene scene) {
		if ( scene == null ) 
			return null;
		
		if (  scene.getUUID() == null ) 
			return null;
		
		if ( Game.saveDirectory == null || Game.saveDirectory.length() == 0 ) 
			return null;
		
		String scenePath = Game.saveDirectory + File.separator + "Scenes" + File.separator;
		File t = new File(scenePath);
		if ( !t.exists() )
			return null;
		
		File t2 = new File(scenePath + File.separator + scene.getUUID().toString());
		if ( !t2.exists() ) 
			return null;
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject externalJSON = (JSONObject) parser.parse(new FileReader(t2));
			SceneInternal internal = new SceneInternal(scene);
			parseJSONInto(externalJSON, internal);
			loadExternalObjects(Game.saveFile);
			return internal;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
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
		ArrayList<LoadedInstance> instances = new ArrayList<LoadedInstance>();
		HashMap<UUID, LoadedInstance> instancesMap = new HashMap<>();
		Map<UUID, Instance> unmodifiedInstances = null;
		
		// Read in the objects from JSON
		readObjects(instances, instancesMap, obj, Game.game(), null);
		
		// Setup instancemap
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
				//UUID parent = inst.Parent;
				Instance p = inst.parent.instance;
				
				// Remove reference to unused
				if ( removeUnusedInstances )
					unmodifiedInstances.remove(inst.instance.getUUID());

				if ( p != null && inst.loaded ) {
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
				Set<Entry<UUID, Instance>> insts = unmodifiedInstances.entrySet();
				Iterator<Entry<UUID, Instance>> iterator = insts.iterator();
				while ( iterator.hasNext() ) {
					Entry<UUID, Instance> entry = iterator.next();
					Instance t = entry.getValue();
					if ( !t.isDestroyed() )
						t.destroy();
				}
			}
			
			return instances.get(0).instance;
		} catch(Exception e ) {
			e.printStackTrace();
			new ErrorWindow("There was a problem reading this file. 002");
		}
		
		return null;
	}

	public static void parseJSONInto(JSONObject obj, Instance rootInstance) {
		// Read in the objects from JSON
		ArrayList<LoadedInstance> instances = new ArrayList<LoadedInstance>();
		HashMap<UUID, LoadedInstance> instancesMap = new HashMap<>();
		LoadedInstance rootloaded = new LoadedInstance(rootInstance);
		instancesMap.put(rootInstance.getUUID(), rootloaded);
		readObjects(instances, instancesMap, obj, rootInstance, rootloaded);
		
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
			if ( inst.parent != null && inst.loaded ) {
				if ( !inst.instance.equals(inst.parent.instance) ) {
					inst.instance.forceSetParent(inst.parent.instance);
				}
			}
		}
		
		// Parent services
		for (int i = 0; i < services.size(); i++) {
			LoadedInstance inst = services.get(i);
			inst.instance.forceSetParent(rootInstance);
		}
	}

	private static void loadObject(HashMap<UUID, LoadedInstance> instancesMap, LoadedInstance inst) {
		if ( inst.loaded )
			return;
		if ( inst.instance == null )
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
			if ( val == null )
				continue;
			
			// Make sure user isn't trying to inject new fields!
			boolean hasField = inst.instance.containsField(LuaValue.valueOf(key));
			if ( !hasField )
				continue;
			
			// Get value for property
			Object value = val.getValue();
			if ( val.pointer ) {
				UUID uuid = (UUID)value;
				if ( uuid != null ) 
					value = getInstanceFromReference(instancesMap, uuid);
			}
			
			// If value is set, set it in ECS
			if ( value != null ) {
				LuaValue lv = (value instanceof LuaValue)?(LuaValue)value:CoerceJavaToLua.coerce(value);
				if ( !inst.instance.get(key).equals(lv) ) {
					//inst.instance.rawset(key, lv);
					inst.instance.forceset(key, lv);
					try{ inst.instance.set(key, lv); } catch(Exception e) {};
				}
			}
		}
	}

	private static void readObjects(ArrayList<LoadedInstance> instances, Map<UUID, LoadedInstance> instancesMap, JSONObject obj, Instance rootInstance, LoadedInstance parent) {
		LoadedInstance loadedInstance = new LoadedInstance();
		loadedInstance.ClassName = (String) obj.get("ClassName");
		loadedInstance.Name = (String) obj.get("Name");
		loadedInstance.uuid = (String) obj.get("UUID");
		loadedInstance.parent = parent;
		if ( parent == null )
			loadedInstance.parent = new LoadedInstance(Game.game());
		loadedInstance.Reference = UUID.fromString(loadedInstance.uuid);
		
		
		// Store instances to map
		instances.add(loadedInstance);
		if ( !instancesMap.containsKey(loadedInstance.Reference)) {
			instancesMap.put(loadedInstance.Reference, loadedInstance);
		}
		
		if ( loadedInstance.ClassName.equals("Game") ) {
			loadedInstance.instance = Game.game();
		}if ( loadedInstance.ClassName.equals("project") ) {
			loadedInstance.instance = Game.project();
		} else {
			// Search directly in game for instance...
			LuaValue temp = rootInstance.get(loadedInstance.ClassName);
			
			// Not found
			if ( temp.isnil() ) {
				
				// Check if UUID is defined
				if ( loadedInstance.uuid != null && loadedInstance.uuid.length() > 0 ) {
	
					// Match instance to uuid
					Instance uuidMatch = Game.getInstanceFromUUID(UUID.fromString(loadedInstance.uuid));
					if ( uuidMatch != null && !uuidMatch.isDestroyed() && uuidMatch.getClassName().eq_b(LuaValue.valueOf(loadedInstance.ClassName)) ) {
						loadedInstance.instance = uuidMatch;
					} else {
						// Create new
						loadedInstance.instance = (Instance) Instance.instance(loadedInstance.ClassName);
					}
				} else {
					// Create new
					loadedInstance.instance = (Instance) Instance.instance(loadedInstance.ClassName);
				}
			} else {
				// Found... probably a service.
				loadedInstance.instance = (Instance) temp;
			}
		}
		
		// Set UUID. Will be verified when added to GameECS later.
		if ( loadedInstance.uuid != null && loadedInstance.uuid.length() > 0 )
			loadedInstance.instance.setUUID(UUID.fromString(loadedInstance.uuid));
		
		// Load children
		JSONArray children = (JSONArray) obj.get("Children");
		for (int i = 0; i < children.size(); i++) {
			readObjects( instances, instancesMap, (JSONObject) children.get(i), rootInstance, loadedInstance);
		}
		
		// Attach properties
		if ( obj.get("Properties") != null ) {
			JSONObject properties = (JSONObject) obj.get("Properties");
			for (Object entry : properties.entrySet()) {
				@SuppressWarnings("unchecked")
				Map.Entry<Object,Object> entry2 = (Entry<Object, Object>) entry;
				String key = entry2.getKey().toString();
				Object t = entry2.getValue();
				PropertyValue<?> v = PropertyValue.parse(t, instancesMap);
				loadedInstance.properties.put(key, v);
			}
		}
	}
	
	protected static Instance getInstanceFromReference(HashMap<UUID, LoadedInstance> instancesMap, UUID uuid) {
		LoadedInstance loaded = instancesMap.get(uuid);
		if ( loaded != null )
			return loaded.instance;
		
		// Now search for instance by UUID if it wasn't found before.
		return Game.getInstanceFromUUID(uuid);
	}
	
	static class LoadedInstance {
		public boolean loaded;
		public String Name;
		public String ClassName;
		public UUID Reference;
		public LoadedInstance parent;
		public Instance instance;
		private String uuid;
		
		public HashMap<String,PropertyValue<?>> properties = new HashMap<String,PropertyValue<?>>();
		
		public LoadedInstance() {
			//
		}
		
		public LoadedInstance(Instance inst) {
			this.instance = inst;
			
			if ( inst.getUUID() != null )
				this.uuid = inst.getUUID().toString();
			
			this.Name = inst.getName();
			this.ClassName = inst.getClassName().toString();
			this.loaded = true;
		}
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

		public static PropertyValue<?> parse(Object fieldKey, Map<UUID, LoadedInstance> instancesMap) {
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
				
				// Match value to an object reference
				if ( j.get("Type").equals("Reference") ) {
					String uuidStr = (String)j.get("Value");
					if ( uuidStr != null && uuidStr.length() > 0 ) {
						UUID uuid = UUID.fromString(uuidStr);
						
						LoadedInstance t1 = instancesMap.get(uuid);
						if ( t1 == null || t1.instance == null ) {
							Instance temp = Game.getInstanceFromUUID(uuid);
							if ( temp != null )
								instancesMap.put(uuid, new LoadedInstance(Game.getInstanceFromUUID(uuid)));
						}
						
						return new PropertyValue<UUID>(uuid, true);
					}
				}
				
				// Match value to datatype
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
