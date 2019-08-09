package ide.layout.windows;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NFDPathSet;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.Game;
import engine.GameSubscriber;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.AnimationController;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.Folder;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.GlobalScript;
import engine.lua.type.object.insts.LocalScript;
import engine.lua.type.object.insts.Mesh;
import engine.lua.type.object.insts.PhysicsObject;
import engine.lua.type.object.insts.PlayerPhysics;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.Script;
import engine.lua.type.object.insts.Texture;
import engine.lua.type.object.services.Assets;
import engine.util.FileUtils;
import ide.IDE;
import ide.layout.IdePane;
import ide.layout.windows.icons.Icons;
import lwjgui.collections.ObservableList;
import lwjgui.scene.Node;
import lwjgui.scene.control.ContextMenu;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.control.SeparatorMenuItem;
import lwjgui.scene.control.TreeBase;
import lwjgui.scene.control.TreeItem;
import lwjgui.scene.control.TreeNode;
import lwjgui.scene.control.TreeView;

public class IdeExplorer extends IdePane implements GameSubscriber {
	private ScrollPane scroller;
	private TreeView<Instance> tree;

	private HashMap<Instance, SortedTreeItem<Instance>> instanceMap;
	private HashMap<Instance, SortedTreeItem<Instance>> instanceMapTemp;
	private HashMap<SortedTreeItem<Instance>,Instance> treeItemMap;
	private ArrayList<SortedTreeItem<Instance>> treeItems;
	private boolean updating;
	
	private static HashMap<Class<? extends Instance>, Integer> priority = new HashMap<>();
	
	static {
		priority.put(Folder.class, 20);

		priority.put(Camera.class, 50);
		
		priority.put(ScriptBase.class, 35);
		priority.put(Script.class, 35);
		priority.put(LocalScript.class, 35);
		priority.put(GlobalScript.class, 35);

		priority.put(GameObject.class, 8);
		
		priority.put(AnimationController.class, 7);

		priority.put(PhysicsBase.class, 6);
		priority.put(PhysicsObject.class, 6);
		priority.put(PlayerPhysics.class, 6);
	}
	
	protected static int getPriority(Class<? extends Instance> cls) {
		Integer ret = priority.get(cls);
		if ( ret == null )
			return 0;
		
		return ret;
	}
	
	public IdeExplorer() {
		super("Explorer New", true);

		this.scroller = new ScrollPane();
		this.scroller.setFillToParentHeight(true);
		this.scroller.setFillToParentWidth(true);
		this.getChildren().add(scroller);

		tree = new SortedTreeView<Instance>();
		this.scroller.setContent(tree);
		
		tree.setOnSelectItem(event -> {
			if ( updating )
				return;
			
			TreeItem<Instance> item = event.object;
			Instance inst = item.getRoot();
			Game.select(inst);
		});
		tree.setOnDeselectItem(event -> {
			if ( updating )
				return;
			
			TreeItem<Instance> item = event.object;
			Instance inst = item.getRoot();
			Game.deselect(inst);
		});
		
		instanceMap = new HashMap<>();
		treeItemMap = new HashMap<>();
		instanceMapTemp = new HashMap<>();
		treeItems = new ArrayList<SortedTreeItem<Instance>>();
		
		Game.getGame().subscribe(this);
		update(true);

		AtomicLong last = new AtomicLong();
		AtomicLong bigUpdate = new AtomicLong();
		Game.runService().renderSteppedEvent().connect((args)->{
			long now = System.currentTimeMillis();
			
			// Little update
			if ( now-last.get() > 200 ) {
				last.set(System.currentTimeMillis());
				update(false);
			}
			
			// Big update
			if ( now-bigUpdate.get() > 1000 ) {
				bigUpdate.set(System.currentTimeMillis());
				update(true);
			}
			
			// Forced update
			if ( requiresUpdate ) {
				updating = false;
				lastUpdate = -1;
				update(false);
			}
		});
		
		Game.userInputService().inputBeganEvent().connect((args)->{
			if ( args[0].get("KeyCode").eq_b(LuaValue.valueOf(GLFW.GLFW_KEY_Q))) {
				System.out.println("Pressed Q");
				update(false);
			}
		});
	}

	private long lastUpdate = -1;
	private boolean requiresUpdate;

	private void update(boolean b) {
		if ( updating )
			return;

		// Non important updates only happen at MOST every 50 ms
		if (System.currentTimeMillis()-lastUpdate < 50 && !b ) {
			//System.out.println("Blocked excess unimportant update.");
			return;
		}
		
		if ( b ) {
			requiresUpdate = true;
			//System.out.println("Deferring explorer update until next frame.");
			return;
		}
		
		lastUpdate = System.currentTimeMillis();
		updating = true;
		
		// Refresh the tree
		if ( b || treeItems.size() == 0 || requiresUpdate ) {
			instanceMapTemp.clear();
			instanceMapTemp.putAll(instanceMap);
			instanceMap.clear();

			treeItemMap.clear();
			treeItems.clear();
			
			list(tree, Game.game());
		}
		
		// Handle selections
		for (int i = 0; i < treeItems.size(); i++) {
			tree.deselectItem(treeItems.get(i));

			// Update names
			TreeItem<Instance> item = treeItems.get(i);
			String name = item.getRoot().getName();
			item.setText(name);
		}
		List<Instance> selected = Game.selected();
		for (int i = 0; i < selected.size(); i++) {
			Instance sel = selected.get(i);
			TreeItem<Instance> t = instanceMap.get(sel);
			if ( t != null ) {
				tree.selectItem(t);
			}
		}

		requiresUpdate = false;
		updating = false;
	}
	
	private void list(TreeBase<Instance> treeItem, Instance root) {
		// Remove all the items in this tree item that are no longer parented to it
		ObservableList<TreeItem<Instance>> items = treeItem.getItems();
		for (int i = 0; i < items.size(); i++) {
			TreeItem<Instance> item = items.get(i);
			Instance obj = item.getRoot();
			LuaValue par = obj.getParent();
			if( par == null || par.isnil() || par != root ) {
				items.remove(item);
				instanceMapTemp.remove(obj);
			}
		}
		
		// Start adding items to tree
		List<Instance> c = root.getChildren();
		for ( int i = 0; i < c.size(); i++) {
			Instance inst = c.get(i);
			
			// Get the tree item
			SortedTreeItem<Instance> newTreeItem = instanceMapTemp.get(inst);
			if ( newTreeItem == null ) {
				// What graphic does it need?
				Node graphic = Icons.icon_wat.getView();
				if ( inst instanceof TreeViewable )
					graphic = ((TreeViewable)inst).getIcon().getView();
				
				// New one
				newTreeItem = new SortedTreeItem<Instance>(inst, graphic);

				// Create context menu
				ContextMenu con = getContetxMenu(inst);
				newTreeItem.setContextMenu(con);
				
				// Open a script
				newTreeItem.setOnMouseClicked(event -> {
					int clicks = event.getClickCount();
					if ( clicks == 2 ) {
						if ( inst instanceof ScriptBase ) {
							IdeLuaEditor lua = new IdeLuaEditor((ScriptBase) inst);
							IDE.layout.getCenter().dock(lua);
						}
					}
				});
			}
			
			// Add this item in if it was reparented.
			Instance obj = newTreeItem.getRoot();
			if ( obj == inst && !treeItem.getItems().contains(newTreeItem) ) {
				treeItem.getItems().add(newTreeItem);
			}
			
			// Update name
			String name = newTreeItem.getRoot().getName();
			newTreeItem.setText(name);
			
			// cache it for easier lookups
			instanceMap.put(inst, newTreeItem);
			treeItemMap.put(newTreeItem, inst);
			treeItems.add(newTreeItem);
			
			// Look ma it's recursion!
			list(newTreeItem, inst);
		}
		
		if ( treeItem instanceof SortedTreeItem ) {
			((SortedTreeItem)treeItem).sort();
		}
	}
	
	private ContextMenu getContetxMenu(Instance inst) {
		ContextMenu c = new ContextMenu();
		c.setAutoHide(false);

		// Cut
		MenuItem cut = new MenuItem("Cut", Icons.icon_cut.getView());
		cut.setOnAction(event -> {
			if ( inst.isInstanceable() ) {
				Instance t = inst.clone();
				if ( t == null || t.isnil() )
					return;
				Game.copiedInstance = t;
				inst.destroy();
			}
		});
		c.getItems().add(cut);

		// Copy
		MenuItem copy = new MenuItem("Copy", Icons.icon_copy.getView());
		copy.setOnAction(event -> {
			if ( inst.isInstanceable() ) {
				Instance t = inst.clone();
				if ( t == null || t.isnil() )
					return;
				Game.copiedInstance = t;
			}
		});
		c.getItems().add(copy);

		// Paste
		MenuItem paste = new MenuItem("Paste", Icons.icon_paste.getView());
		paste.setOnAction(event -> {
			Instance t = Game.copiedInstance;
			if ( t == null )
				return;
			t.clone().forceSetParent(inst);
		});
		c.getItems().add(paste);

		// Copy
		MenuItem duplicate = new MenuItem("Duplicate", Icons.icon_copy.getView());
		duplicate.setOnAction(event -> {
			if ( inst.isInstanceable() ) {
				Instance t = inst.clone();
				if ( t == null || t.isnil() )
					return;
				t.forceSetParent(inst.getParent());
			}
		});
		c.getItems().add(duplicate);
		
		// Separate
		c.getItems().add(new SeparatorMenuItem());

		// New Model
		if ( inst instanceof Prefab ) {
			// New Prefab
			MenuItem pref = new MenuItem("Add Model", Icons.icon_wat.getView());
			pref.setOnAction(event -> {
				((Prefab)inst).get("AddModel").invoke(LuaValue.NIL,LuaValue.NIL,LuaValue.NIL);
			});
			c.getItems().add(pref);

			// Create gameobject
			MenuItem gobj = new MenuItem("Create GameObject", Icons.icon_gameobject.getView());
			gobj.setOnAction(event -> {
				GameObject g = new GameObject();
				g.setPrefab((Prefab) inst);
				g.setParent(Game.workspace());
			});
			c.getItems().add(gobj);
			
			
		}
		
		// Asset functions
		if ( inst instanceof Assets ) {
			
			// New Prefab
			MenuItem prefi = new MenuItem("Import Prefab", Icons.icon_model.getView());
			prefi.setOnAction(event -> {
				String path = "";
				PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
				int result = NativeFileDialog.NFD_OpenDialog(Mesh.getFileTypes(), new File("").getAbsolutePath(), outPath);
				if ( result == NativeFileDialog.NFD_OKAY ) {
					path = outPath.getStringUTF8(0);
					Game.assets().importPrefab(path);
				} else {
					return;
				}
			});
			c.getItems().add(prefi);
			
			// Import Texture
			MenuItem texi = new MenuItem("Import Texture", Icons.icon_texture.getView());
			texi.setOnAction(event -> {
				NFDPathSet outPaths = NFDPathSet.calloc();
				int result = NativeFileDialog.NFD_OpenDialogMultiple(Texture.getFileTypes(), new File("").getAbsolutePath(), outPaths);
				if ( result == NativeFileDialog.NFD_OKAY ) {
					long count = NativeFileDialog.NFD_PathSet_GetCount(outPaths);
					for (long i = 0; i < count; i++) {
						String path = NativeFileDialog.NFD_PathSet_GetPath(outPaths, i);
						Instance t = Game.assets().importTexture(path);
						File ff = new File(path);
						if ( ff.exists() ) {
							t.forceSetName(FileUtils.getFileNameWithoutExtension(ff.getName()));
						}
					}
				} else {
					return;
				}
			});
			c.getItems().add(texi);
			
			// Separate
			c.getItems().add(new SeparatorMenuItem());
			
			// New Prefab
			MenuItem pref = new MenuItem("New Prefab", Icons.icon_model.getView());
			pref.setOnAction(event -> {
				Game.getService("Assets").get("NewPrefab").invoke();
			});
			c.getItems().add(pref);
			
			// New Mesh
			MenuItem mesh = new MenuItem("New Mesh", Icons.icon_mesh.getView());
			mesh.setOnAction(event -> {
				Game.getService("Assets").get("ImportMesh").invoke(LuaValue.NIL);
			});
			c.getItems().add(mesh);

			// New Material
			MenuItem mat = new MenuItem("New Material", Icons.icon_material.getView());
			mat.setOnAction(event -> {
				Game.getService("Assets").get("NewMaterial").invoke();
			});
			c.getItems().add(mat);

			// New Texture
			MenuItem tex = new MenuItem("New Texture", Icons.icon_texture.getView());
			tex.setOnAction(event -> {
				Game.getService("Assets").get("NewTexture").invoke();
			});
			c.getItems().add(tex);
			
			// Separate
			c.getItems().add(new SeparatorMenuItem());
		}


		// Cut
		MenuItem insert = new MenuItem("Insert Object  \u25ba", Icons.icon_new.getView());
		insert.setOnAction(event -> {
			new InsertWindow();
		});
		c.getItems().add(insert);
		
		return c;
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		update(important);
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}

	
	class SortedTreeView<E> extends TreeView<E> {
		//
	}
	
	class SortedTreeItem<E> extends TreeItem<E> {

		public SortedTreeItem(E root) {
			super(root);
		}
		
		public SortedTreeItem(E root, Node node) {
			super(root, node);
		}
		
		protected void sort() {
			ArrayList<TreeNode<E>> nodules = new ArrayList<TreeNode<E>>();
			for (int i = 0; i < nodes.size(); i++) {
				nodules.add(nodes.get(i));
			}
			
			Collections.sort(nodules, new Comparator<TreeNode<E>>() {

				@SuppressWarnings("unchecked")
				@Override
				public int compare(TreeNode<E> o1, TreeNode<E> o2) {
					int priority1 = getPriority((Class<? extends Instance>) o1.getItem().getRoot().getClass());
					int priority2 = getPriority((Class<? extends Instance>) o2.getItem().getRoot().getClass());
					
					return (priority1<priority2)?1:((priority1==priority2)?0:-1);
				}
				
			});
			
			this.nodes = new ObservableList<TreeNode<E>>();
			for (int i = 0; i < nodules.size(); i++) {
				nodes.add(nodules.get(i));
			}
		}
	}
}
