package engine.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.Game;
import engine.InternalRenderThread;
import engine.gl.mesh.BufferedMesh;
import engine.lua.type.LuaValuetype;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeInvisible;
import engine.lua.type.object.insts.Mesh;
import engine.util.FileUtils;
import ide.IDE;
import lwjgui.LWJGUI;
import lwjgui.LWJGUIUtil;
import lwjgui.geometry.Insets;
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
	private static int REFID = 0;
	private static ArrayList<SavedInstance> inst;

	public static void requestSave(Runnable after) {
		long win = LWJGUIUtil.createOpenGLCoreWindow("Save Dialog", 300, 100, false, true);
		Window window = LWJGUI.initialize(win);
		window.setCanUserClose(false);

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
		bottom.setBackground(Theme.currentTheme().getControlAlt());
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
		JSONObject gameJSON = getGameJSON();
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
	
	public static JSONObject getGameJSON() {
		JSONObject ret = null;
		try {
			Instance game = (Instance)Game.game();
			inst = getSavedInstances(game);
			ret = inst.get(0).toJSON();
		} catch(Exception e ) {
			e.printStackTrace();
		}
		
		return ret;
	}

	private static ArrayList<SavedInstance> getSavedInstances(Instance root) {
		ArrayList<SavedInstance> ret = new ArrayList<SavedInstance>();
		
		if ( !root.get("Archivable").checkboolean() )
			return ret;

		ret.add(new SavedInstance(root));

		List<Instance> children = root.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			ArrayList<SavedInstance> t = getSavedInstances(child);
			for (int j = 0; j < t.size(); j++) {
				ret.add(t.get(j));
			}
		}

		return ret;
	}

	static class SavedInstance {
		final Instance instance;
		final SavedReference reference;

		public SavedInstance(Instance child) {
			this.instance = child;
			this.reference = new SavedReference(REFID++);
		}

		@SuppressWarnings("unchecked")
		public JSONObject toJSON() {
			if ( !instance.get("Archivable").checkboolean() )
				return null;
			
			SavedInstance parent = null;
			if ( !instance.getParent().isnil() ) {
				parent = getSavedInstance((Instance) instance.getParent());
			}

			List<Instance> children = instance.getChildren();
			JSONArray childArray = new JSONArray();
			for (int i = 0; i < children.size(); i++) {
				Instance child = children.get(i);
				if ( child instanceof TreeInvisible )
					continue;

				SavedInstance sinst = getSavedInstance(child);
				if ( sinst != null ) {
					JSONObject json = sinst.toJSON();
					if ( json != null ) {
						childArray.add(json);
					}
				}
			}

			JSONObject p = new JSONObject();
			String[] fields = instance.getFields();
			for (int i = 0; i < fields.length; i++) {
				String field = fields[i];
				if ( field.equals("Name") || field.equals("ClassName") || field.equals("Parent") )
					continue;

				p.put(field, fieldToJSON(instance.get(field)));
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
		public SavedReference(int ref) {
			this.put("Type", "Reference");
			this.put("Value", ref);
		}

		public int getReference() {
			return (int) this.get("Reference");
		}
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
			return getSavedInstance((Instance) luaValue).reference;
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

	protected static SavedInstance getSavedInstance(Instance instance) {
		for (int i = 0; i < inst.size(); i++) {
			SavedInstance s = inst.get(i);
			if ( s.instance.equals(instance) ) {
				return s;
			}
		}
		return null;
	}

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
			filePath = filePath.replace("%PROJECT%", new File(Game.saveDirectory).getAbsolutePath());
			
			if ( filePath != null && filePath.length() > 0 ) {
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
							
							a.rawset("FilePath", dest.replace(p.getParentFile().getParentFile().getAbsolutePath(), "%PROJECT%"));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				if ( a instanceof Mesh ) {
					Mesh mesh = (Mesh) a;
					BufferedMesh bufferedMesh = mesh.getMesh();
					if ( bufferedMesh != null ) {
						File d = getFreeFile( path, "Mesh_", ".mesh");
						if ( d != null ) {
							String dest = d.getAbsolutePath();
							
							BufferedMesh.Export(bufferedMesh, dest);
							
							a.rawset("FilePath", dest.replace(p.getParentFile().getParentFile().getAbsolutePath(), "%PROJECT%"));
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
