package ide.layout.windows;

import java.io.File;
import java.util.List;

import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NFDPathSet;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.Game;
import engine.GameSubscriber;
import engine.lua.type.object.Instance;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeInvisible;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Prefab;
import engine.lua.type.object.insts.Script;
import engine.lua.type.object.services.Assets;
import engine.util.FileUtils;
import ide.IDE;
import ide.layout.IdePane;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.collections.ObservableList;
import lwjgui.scene.control.ContextMenu;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.control.TreeBase;
import lwjgui.scene.control.TreeItem;
import lwjgui.scene.control.TreeView;

public class IdeExplorer extends IdePane implements GameSubscriber {
	private ScrollPane scroller;
	private TreeView<Instance> tree;

	private ObservableList<TreeItem<Instance>> cache1;
	private ObservableList<TreeItem<Instance>> cache2;

	public IdeExplorer() {
		super("Explorer", true);

		this.scroller = new ScrollPane();
		this.scroller.setFillToParentHeight(true);
		this.scroller.setFillToParentWidth(true);
		this.getChildren().add(scroller);

		tree = new TreeView<Instance>();
		this.scroller.setContent(tree);

		tree.setOnSelectItem(event -> {
			TreeItem<Instance> item = event.object;
			Instance inst = item.getRoot();
			Game.select(inst);
		});
		tree.setOnDeselectItem(event -> {
			TreeItem<Instance> item = event.object;
			Instance inst = item.getRoot();
			Game.deselect(inst);
		});

		Game.getGame().subscribe(this);

		cache1 = new ObservableList<TreeItem<Instance>>();
		cache2 = new ObservableList<TreeItem<Instance>>();

		update();
	}

	public void update() {
		LWJGUI.runLater(() -> {
			cache2.clear();
			synchronized(children) {
				list(tree, (Instance)Game.game());
			}

			cache1.clear();
			for (int i = 0; i < cache2.size(); i++) {
				cache1.add(cache2.get(i));
			}
			position(getParent());
		});
	}

	private void list(TreeBase<Instance> treeItem, Instance root) {
		// Remove objects whos parents are gone/changed
		for (int i = 0; i < treeItem.getItems().size(); i++) {
			TreeItem<Instance> t = treeItem.getItems().get(i);
			boolean b = false;
			Instance par = null;
			if ( treeItem instanceof TreeItem ) {
				par = ((TreeItem<Instance>)treeItem).getRoot();
				if ( !par.equals(t.getRoot().getParent()) ) {
					b = true;
				}
			}
			
			if ( t.getRoot() == null || t.getRoot().equals(LuaValue.NIL) || t.getRoot().getParent().equals(LuaValue.NIL) || b) {
				treeItem.getItems().remove(t);
				i--;
				
				if ( b ) {
					cache2.remove(t);
					cache1.remove(t);
				}
			}
		}
		
		// Put instances in explorer
		List<Instance> instances = root.getChildren();
		for (int i = 0; i < instances.size(); i++) {
			Instance inst = instances.get(i);
			if ( inst instanceof TreeInvisible )
				continue;
			
			Icons icon = Icons.icon_wat;
			if ( inst instanceof TreeViewable ) {
				icon = ((TreeViewable)inst).getIcon();
			}

			TreeItem<Instance> cached = getCachedInstance(inst);
			TreeItem<Instance> item = cached;

			if ( item == null ) {
				item = new TreeItem<Instance>(inst, icon.getView());

				item.setOnMouseClicked(event -> {
					int clicks = event.getClickCount();
					if ( clicks == 2 ) {
						if ( inst instanceof ScriptBase ) {
							IdeLuaEditor lua = new IdeLuaEditor((ScriptBase) inst);
							IDE.layout.getCenter().dock(lua);
						}
					}
				});

				// Create context menu
				ContextMenu c = getContetxMenu(inst);
				item.setContextMenu(c);
			} else {
				cache1.remove(item);
			}
			
			if ( !treeItem.getItems().contains(item) ) {
				treeItem.getItems().add(item);
			}

			if ( Game.isSelected(inst) ) {
				tree.selectItem(item);
			} else {
				tree.deselectItem(item);
			}

			item.setText(inst.getName());
			cache2.add(item);
			list(item, inst);
		}
	}

	private ContextMenu getContetxMenu(Instance inst) {
		ContextMenu c = new ContextMenu();

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
				int result = NativeFileDialog.NFD_OpenDialog("obj,fbx", new File("").getAbsolutePath(), outPath);
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
				int result = NativeFileDialog.NFD_OpenDialogMultiple("png,bmp,tga,jpg,jpeg,hdr", new File("").getAbsolutePath(), outPaths);
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
		}

		return c;
	}

	private TreeItem<Instance> getCachedInstance(Instance in) {
		for (int i = 0; i < cache1.size(); i++) {
			TreeItem<Instance> c = cache1.get(i);
			if ( c.getRoot().equals(in) ) {
				return c;
			}
		}
		return null;
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		update();
	}
}
