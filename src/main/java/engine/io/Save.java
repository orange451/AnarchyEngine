package engine.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.FilePath;
import engine.Game;
import engine.InternalRenderThread;
import engine.gl.mesh.BufferedMesh;
import engine.lua.network.internal.NonReplicatable;
import engine.lua.type.LuaValuetype;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeInvisible;
import engine.lua.type.object.insts.Mesh;
import engine.util.FileUtils;
import ide.IDE;
import lwjgui.LWJGUI;
import lwjgui.LWJGUIUtil;
import lwjgui.geometry.Pos;
import lwjgui.scene.Scene;
import lwjgui.scene.Window;
import lwjgui.scene.control.Button;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.BorderPane;
import lwjgui.scene.layout.HBox;
import lwjgui.scene.layout.StackPane;
import lwjgui.theme.Theme;

public class Save {
	private static long REFID = Integer.MAX_VALUE;
	private static LinkedList<SavedInstance> inst;
	private static HashMap<Instance, SavedInstance> instanceMap;

	public static void requestSave(Runnable after) {
		long win = LWJGUIUtil.createOpenGLCoreWindow("Save Dialog", 300, 100, false, true);
		Window window = LWJGUI.initialize(win);
		window.setCanUserClose(false);
		window.show();

		Scene scene = window.getScene();

		// Create root pane
		BorderPane root = new BorderPane();
		scene.setRoot(root);

		// Create a label
		Label l = new Label("Unsaved changes. Would you like to save?");
		root.setCenter(l);

		StackPane bottom = new StackPane();
		bottom.setPrefHeight(32);
		bottom.setAlignment(Pos.CENTER);
		bottom.setFillToParentWidth(true);
		bottom.setBackground(Theme.current().getControlAlt());
		root.setBottom(bottom);
		
		HBox t = new HBox();
		t.setAlignment(Pos.CENTER);
		t.setBackground(null);
		t.setSpacing(16);
		bottom.getChildren().add(t);

		// Create a button
		Button b = new Button("Yes");
		b.setMinWidth(64);
		b.setOnAction(event -> {
			GLFW.glfwSetWindowShouldClose(win, true);
			if ( save() ) {
				InternalRenderThread.runLater(()->{
					after.run();
				});
			}
		});
		t.getChildren().add(b);

		// Create a button
		Button b2 = new Button("No");
		b2.setMinWidth(64);
		b2.setOnAction(event -> {
			GLFW.glfwSetWindowShouldClose(win, true);
			InternalRenderThread.runLater(()->{
				after.run();
			});
		});
		t.getChildren().add(b2);

		// Create a button
		Button b3 = new Button("Cancel");
		b3.setMinWidth(64);
		b3.setOnAction(event -> {
			GLFW.glfwSetWindowShouldClose(win, true);
		});
		t.getChildren().add(b3);
	}
	
	public static boolean save() {
		return save(false);
	}

	public static boolean save(boolean saveAs) {
		String path = Game.saveFile;

		// Make projects folder
		File projects = new File("Projects");
		if ( !projects.exists() ) {
			projects.mkdirs();
		}

		// Get save path if it's not already set.
		if ( path == null || path.length() == 0 || saveAs ) {
			PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
			int result = NativeFileDialog.NFD_SaveDialog("json", projects.getAbsolutePath(), outPath);
			if ( result == NativeFileDialog.NFD_OKAY ) {
				path = outPath.getStringUTF8(0);
			} else {
				return false;
			}
			
			if ( !path.endsWith(".json") ) {
				path = path+".json";
			}
		}

		// Setup directory folder
		String jsonFile = FileUtils.getFileNameFromPath(path);
		String gameDirectoryPath = FileUtils.getFileDirectoryFromPath(path) + File.separator + FileUtils.getFileNameWithoutExtension(jsonFile);
		File f = new File(path);
		if ( f.exists() ) {
			gameDirectoryPath = FileUtils.getFileDirectoryFromPath(path);
		}
		File gameDirectory = new File(gameDirectoryPath);
		if ( !gameDirectory.exists() ) {
			gameDirectory.mkdirs();
		}

		// Update project path
		path = gameDirectory.getAbsolutePath() + File.separator + jsonFile;
		System.out.println(gameDirectory);
		System.out.println(path);
		Game.saveDirectory = gameDirectory.getAbsolutePath();
		Game.saveFile = path;
		GLFW.glfwSetWindowTitle(IDE.window, IDE.TITLE + " [" + FileUtils.getFileDirectoryFromPath(path) + "]");

		// Resources folder
		File resourcesFolder = new File(gameDirectory + File.separator + "Resources");
		if ( !resourcesFolder.exists() ) {
			resourcesFolder.mkdir();
		}
		writeResources(resourcesFolder);

		// Start saving process
		REFID = 0;		
		JSONObject gameJSON = getInstanceJSONRecursive(false, true, Game.game());
		Game.changes = false;

		try {
			// Get scene as JSON string
			String text = gameJSON.toJSONString();

			// Write to file
			File file=new File(path);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(text.getBytes());
			bos.flush();
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	/**
	 * Returns the entire game represented as a JSON Object.
	 * @return
	 */
	public static JSONObject getGameJSON() {
		return getInstanceJSONRecursive(true, true, Game.game());
	}
	
	/**
	 * Returns the instance and all of its children represented as JSON Objects.
	 * @param instance
	 * @return
	 */
	public static JSONObject getInstanceJSONRecursive(boolean saveArchivable, boolean saveSID, Instance instance) {
		JSONObject ret = null;
		try {
			instanceMap = new HashMap<>();
			inst = getSavedInstances(saveArchivable, saveSID, instance);
			ret = inst.getFirst().toJSON(saveArchivable, saveSID);
		} catch(Exception e ) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	/**
	 * Returns the instance represented as a JSON Object.
	 * @param instance
	 * @return
	 */
	public static JSONObject getInstanceJSON(boolean saveArchivable, boolean saveSID, Instance instance) {
		JSONObject ret = null;
		try {
			
			if ( !instance.isArchivable() && !saveArchivable )
				return ret;

			ret = new SavedInstance(saveSID, instance).toJSON(saveArchivable, saveSID);
		} catch(Exception e ) {
			e.printStackTrace();
		}
		
		return ret;
	}

	private static LinkedList<SavedInstance> getSavedInstances(boolean saveArchivable, boolean saveSID, Instance root) {
		LinkedList<SavedInstance> ret = new LinkedList<SavedInstance>();
		
		if ( !root.isArchivable() && !saveArchivable )
			return ret;

		SavedInstance savedRoot = new SavedInstance(saveSID, root);
		ret.add(savedRoot);
		instanceMap.put(root, savedRoot);

		List<Instance> children = root.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			List<SavedInstance> t = getSavedInstances(saveArchivable, saveSID, child);
			
			ListIterator<SavedInstance> listIterator = t.listIterator();
			while(listIterator.hasNext()) {
				SavedInstance si = listIterator.next();
				ret.add(si);
			}
		}

		return ret;
	}

	static class SavedInstance {
		final Instance instance;
		final SavedReference reference;

		public SavedInstance(boolean saveSID, Instance child) {
			this.instance = child;
			long refId = saveSID?child.getSID():-1;
			if ( refId == -1 )
				refId = REFID++;
			
			this.reference = new SavedReference(refId);
		}

		@SuppressWarnings("unchecked")
		public JSONObject toJSON(boolean saveArchivable, boolean saveSID) {
			if ( !instance.isArchivable() && !saveArchivable )
				return null;
			
			SavedInstance parent = null;
			if ( !instance.getParent().isnil() ) {
				parent = instanceMap.get((Instance) instance.getParent());
			}

			List<Instance> children = instance.getChildren();
			JSONArray childArray = new JSONArray();
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				if ( child instanceof TreeInvisible )
					continue;

				SavedInstance sinst = instanceMap.get(child);
				if ( sinst != null ) {
					JSONObject json = sinst.toJSON(saveArchivable, saveSID);
					if ( json != null ) {
						childArray.add(json);
					}
				}
			}

			JSONObject p = new JSONObject();
			LuaValue[] fields = instance.getFields();
			for (int i = 0; i < fields.length; i++) {
				String field = fields[i].toString();
				if ( field.equals("Name") || field.equals("ClassName") || field.equals("Parent") )
					continue;
				
				if ( field.equals("SID") && !saveSID )
					continue;

				if ( !field.equals("SID") && Game.isRunning() && instance instanceof NonReplicatable ) {
					// Protect the fields of non replicatable objects being sent when running.
					p.put(field, fieldToJSONBlank(instance.get(field)));
				} else {
					p.put(field, fieldToJSON(instance.get(field)));
				}
			}

			JSONObject j = new JSONObject();
			j.put("Reference", reference);
			j.put("ClassName", instance.get("ClassName").toString());
			j.put("Name", instance.getName());
			if ( parent != null )
				j.put("Parent", parent.reference);
			j.put("Children", childArray);
			j.put("Properties", p);

			return j;
		}
	}

	@SuppressWarnings("serial")
	static class SavedReference extends JSONObject {
		@SuppressWarnings("unchecked")
		public SavedReference(long ref) {
			this.put("Type", "Reference");
			this.put("Value", ref);
		}
	}

	@SuppressWarnings("serial")
	static class HashReference extends SavedReference {
		@SuppressWarnings("unchecked")
		public HashReference(Instance instance) {
			super(instance.getSID());
			
			this.put("Hash", instance.hashFields());
		}
	}

	@SuppressWarnings("unchecked")
	protected static Object fieldToJSONBlank(LuaValue luaValue) {
		if ( luaValue.isstring() )
			return "";
		if ( luaValue.isboolean() )
			return false;
		if ( luaValue.isnumber() )
			return 0.0;
		
		if ( luaValue instanceof Instance )
			return null;
		
		// Vectorx/Colors/etc
		if ( luaValue instanceof LuaValuetype ) {
			try {
				JSONObject t = new JSONObject();
				t.put("ClassName", ((LuaValuetype)luaValue).typename());
				t.put("Data", ((LuaValuetype)luaValue).getClass().newInstance().toJSON());

				JSONObject j = new JSONObject();
				j.put("Type", "Datatype");
				j.put("Value", t);
				return j;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	protected static Object fieldToJSON(LuaValue luaValue) {
		if ( luaValue.isstring() )
			return luaValue.toString();
		if ( luaValue.isboolean() )
			return luaValue.checkboolean();
		if ( luaValue.isnumber() )
			return luaValue.todouble();

		// Instances in the game
		if ( luaValue instanceof Instance ) {
			SavedInstance svd = instanceMap.get((Instance) luaValue);
			if ( svd != null ) {
				return svd.reference;
			} else {
				return new HashReference((Instance) luaValue);
			}
		}

		// Vectorx/Colors/etc
		if ( luaValue instanceof LuaValuetype ) {
			JSONObject t = new JSONObject();
			t.put("ClassName", ((LuaValuetype)luaValue).typename());
			t.put("Data", ((LuaValuetype)luaValue).toJSON());

			JSONObject j = new JSONObject();
			j.put("Type", "Datatype");
			j.put("Value", t);
			return j;
		}

		return null;
	}

	/*protected static SavedInstance getSavedInstance(Instance instance) {
		for (int i = 0; i < inst.size(); i++) {
			SavedInstance s = inst.get(i);
			if ( s.instance.equals(instance) ) {
				return s;
			}
		}
		return null;
	}*/

	protected static void writeResources(File resourcesFolder) {
		String resourcesPath = resourcesFolder.getAbsolutePath();
		copyAssets( resourcesPath + File.separator + "Textures", Game.assets().getTextures());
		copyAssets( resourcesPath + File.separator + "Meshes", Game.assets().getMeshes());
	}
	
	private static boolean deleteDirectory(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}

	private static void copyAssets(String path, List<AssetLoadable> assets) {
		File p = new File(path);
		if ( !p.exists() )
				p.mkdirs();
		
		ArrayList<String> writtenTo = new ArrayList<String>();
		
		for (int i = 0; i < assets.size(); i++) {
			AssetLoadable a = assets.get(i);
			
			String filePath = a.getFilePath();
			boolean localFile = filePath.contains(FilePath.PROJECT_IDENTIFIER);
			filePath = filePath.replace(FilePath.PROJECT_IDENTIFIER, new File(Game.saveDirectory).getAbsolutePath());
			
			if ( filePath != null && filePath.length() > 3 ) {
				String fileName = FileUtils.getFileNameFromPath(filePath);
				String fileNameEx = FileUtils.getFileNameWithoutExtension(fileName);
				String fileExtension = FileUtils.getFileExtension(filePath);
				String dest = path + File.separator + fileName;
	
				// Get final destination
				File d = new File(dest);
				int aa = 0;
				while ( writtenTo.contains(dest) ) {
					dest = path + File.separator + fileNameEx + aa + fileExtension;
					d = new File(dest);
					aa++;
				}
				writtenTo.add(dest);
	
				if ( filePath != null && filePath.length() > 0 ) {
					try {
						
						// Get the source file
						File s = new File(filePath);
						if ( s.exists() ) {
							Files.copy(s.toPath(), d.toPath(), StandardCopyOption.REPLACE_EXISTING);
							
							a.rawset("FilePath", dest.replace(p.getParentFile().getParentFile().getAbsolutePath(), FilePath.PROJECT_IDENTIFIER));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				
				// Mesh without being backed by a file. Save its data to a file!
				if ( a instanceof Mesh ) {
					Mesh mesh = (Mesh) a;
					BufferedMesh bufferedMesh = mesh.getMesh();
					if ( bufferedMesh != null ) {
						File d = getFreeFile( path, "Mesh_", ".mesh");
						if ( d != null ) {
							String dest = d.getAbsolutePath();
							BufferedMesh.Export(bufferedMesh, dest);
							a.rawset("FilePath", dest.replace(p.getParentFile().getParentFile().getAbsolutePath(), FilePath.PROJECT_IDENTIFIER));
						}
					}
				}
			}
		}
	}

	private static File getFreeFile(String path, String name, String extension) {
		File t = null;
		int a = 0;
		while ( t == null || t.exists() ) {
			t = new File(path + File.separator + name + a + extension);
			a++;
		}
		return t;
	}
}
