package ide.layout.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalGameThread;
import engine.lua.history.HistoryChange;
import engine.lua.history.HistorySnapshot;
import engine.lua.type.LuaConnection;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.AnimationController;
import engine.lua.type.object.insts.AnimationData;
import engine.lua.type.object.insts.AssetFolder;
import engine.lua.type.object.insts.AudioPlayer2D;
import engine.lua.type.object.insts.AudioPlayer3D;
import engine.lua.type.object.insts.AudioSource;
import engine.lua.type.object.insts.BoolValue;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.Color3Value;
import engine.lua.type.object.insts.DirectionalLight;
import engine.lua.type.object.insts.NumberValue;
import engine.lua.type.object.insts.Folder;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.GlobalScript;
import engine.lua.type.object.insts.IntValue;
import engine.lua.type.object.insts.LocalScript;
import engine.lua.type.object.insts.Matrix4Value;
import engine.lua.type.object.insts.ObjectValue;
import engine.lua.type.object.insts.PhysicsObject;
import engine.lua.type.object.insts.PlayerPhysics;
import engine.lua.type.object.insts.PointLight;
import engine.lua.type.object.insts.Script;
import engine.lua.type.object.insts.SpotLight;
import engine.lua.type.object.insts.StringValue;
import engine.lua.type.object.insts.Vector2Value;
import engine.lua.type.object.insts.Vector3Value;
import engine.lua.type.object.services.RenderSettings;
import engine.lua.type.object.services.StarterPlayerScripts;
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

public class IdeExplorerNew extends IdePane {
	private ScrollPane scroller;
	private TreeView<Instance> tree;

	private static final LuaValue C_NAME = LuaValue.valueOf("Name");
	private static final LuaValue C_PARENT = LuaValue.valueOf("Parent");
	
	private HashMap<Instance, TreeItem<Instance>> instanceToTreeItemMap;
	private HashMap<TreeItem<Instance>, TreeBase<Instance>> treeItemToParentTreeItemMap;
	private HashMap<TreeItem<Instance>, LuaConnection> treeItemToChangedConnectionMap;
	private List<Instance> selectedCache;
	
	private static HashMap<Class<? extends Instance>, Integer> priority = new HashMap<>();
	
	static {
		priority.put(Camera.class, 50);

		priority.put(StarterPlayerScripts.class, 41);
		priority.put(AssetFolder.class, 41);
		priority.put(RenderSettings.class, 41);
		priority.put(AnimationData.class, 40);
		
		priority.put(ScriptBase.class, 35);
		priority.put(Script.class, 35);
		priority.put(LocalScript.class, 35);
		priority.put(GlobalScript.class, 35);

		priority.put(DirectionalLight.class, 25);
		priority.put(PointLight.class, 24);
		priority.put(SpotLight.class, 23);
		
		priority.put(Folder.class, 20);

		priority.put(GameObject.class, 8);
		priority.put(AnimationController.class, 7);
		priority.put(PhysicsBase.class, 6);
		priority.put(PhysicsObject.class, 6);
		priority.put(PlayerPhysics.class, 6);

		priority.put(IntValue.class, 5);
		priority.put(BoolValue.class, 5);
		priority.put(StringValue.class, 5);
		priority.put(NumberValue.class, 5);
		priority.put(ObjectValue.class, 5);
		priority.put(Color3Value.class, 5);
		priority.put(Vector2Value.class, 5);
		priority.put(Vector3Value.class, 5);
		priority.put(Matrix4Value.class, 5);

		priority.put(AudioSource.class, 4);
		priority.put(AudioPlayer2D.class, 4);
		priority.put(AudioPlayer3D.class, 4);
	}
	
	protected static int getPriority(Class<? extends Instance> cls) {
		Integer ret = priority.get(cls);
		if ( ret == null )
			return 0;
		
		return ret;
	}
	
	public IdeExplorerNew() {
		super("Explorer New", true);

		this.scroller = new ScrollPane();
		this.scroller.setFillToParentHeight(true);
		this.scroller.setFillToParentWidth(true);
		this.getChildren().add(scroller);
		
		// Object Added
		Game.game().descendantAddedEvent().connect((args)->{
			if ( !Game.isLoaded() )
				return;
			buildNode((Instance) args[0]);
		});
		
		// Object removed
		Game.game().descendantRemovedEvent().connect((args)->{
			destroyNode((Instance) args[0]);
		});
		
		// Game reset
		Game.resetEvent().connect((args)-> {
			rebuild();
		});
		
		// First (initial) load
		InternalGameThread.runLater(()->{
			rebuild();
		});
		
		// Selection change
		Game.selectionChanged().connect((args)->{
			List<Instance> selected = Game.selected();
			List<Instance> toSelect = new ArrayList<Instance>();
			List<Instance> toUnselect = new ArrayList<Instance>();
			
			// Prune left
			for (int i = 0; i < selectedCache.size(); i++) {
				Instance alreadySelected = selectedCache.get(i);
				if ( !selected.contains(alreadySelected) )
					toUnselect.add(alreadySelected);
			}
			
			// Prune right
			for (int i = 0; i < selected.size(); i++) {
				Instance potentialNotYetSelected = selected.get(i);
				if ( !selectedCache.contains(potentialNotYetSelected) )
					toSelect.add(potentialNotYetSelected);
			}
			
			// Unselect old ones
			for (int i = 0; i < toUnselect.size(); i++) {
				Instance t = toUnselect.get(i);
				TreeItem<Instance> t2 = instanceToTreeItemMap.get(t);
				tree.deselectItem(t2);
			}
			
			// select new ones
			for (int i = 0; i < toSelect.size(); i++) {
				Instance t = toSelect.get(i);
				TreeItem<Instance> t2 = instanceToTreeItemMap.get(t);
				tree.selectItem(t2);
			}
			
			// set cache
			selectedCache = new ArrayList<Instance>(selected);
		});
		
		// Add user controls
		StandardUserControls.bind(this);
	}
	
	private void clear() {
		if ( this.tree != null ) {
			this.tree.clearSelectedItems();
			this.tree.getItems().clear();
		}
		
		if ( instanceToTreeItemMap != null )
			instanceToTreeItemMap.clear();
		this.instanceToTreeItemMap = new HashMap<>();
		
		if ( treeItemToParentTreeItemMap != null )
			treeItemToParentTreeItemMap.clear();
		this.treeItemToParentTreeItemMap = new HashMap<>();
		
		if ( treeItemToChangedConnectionMap != null )
			treeItemToChangedConnectionMap.clear();
		this.treeItemToChangedConnectionMap = new HashMap<>();
		
		if ( selectedCache != null )
			selectedCache.clear();
		this.selectedCache = new ArrayList<Instance>();

		this.tree = new SortedTreeView<Instance>();
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
	}
	
	private void rebuild() {
		clear();
		
		List<Instance> gameDescendents = Game.game().getDescendantsUnsafe();
		for (int i = 0; i < gameDescendents.size(); i++) {
			if ( i >= gameDescendents.size() )
				continue;
			
			Instance desc = gameDescendents.get(i);
			if ( desc == null )
				continue;
			
			buildNode(desc);
		}
	}
	
	private void destroyNode(Instance instance) {
		TreeItem<Instance> treeItem = instanceToTreeItemMap.get(instance);
		if ( treeItem == null )
			return;
		
		TreeBase<Instance> parentTreeItem = treeItemToParentTreeItemMap.get(treeItem);
		if ( parentTreeItem == null )
			return;
		
		parentTreeItem.getItems().remove(treeItem);
		instanceToTreeItemMap.remove(instance);
		treeItemToParentTreeItemMap.remove(treeItem);
		
		LuaConnection con = treeItemToChangedConnectionMap.get(treeItem);
		if ( con != null )
			con.disconnect();
		treeItemToChangedConnectionMap.remove(treeItem);
	}
	
	private synchronized void buildNode(Instance instance) {
		synchronized(instanceToTreeItemMap) {
			// Get Tree Node
			TreeItem<Instance> treeItem = instanceToTreeItemMap.get(instance);
			if ( treeItem == null ) {
				// What graphic does it need?
				Node graphic = Icons.icon_wat.getView();
				if ( instance instanceof TreeViewable )
					graphic = ((TreeViewable)instance).getIcon().getView();
				
				// CREATE NEW NODE
				treeItem = new SortedTreeItem<Instance>(instance, graphic);
				treeItem.setContextMenu(getContetxMenu(instance));
				
				// Open a script shortcut
				treeItem.setOnMouseClicked(event -> {
					int clicks = event.getClickCount();
					if ( clicks == 2 ) {
						if ( instance instanceof ScriptBase ) {
							IDE.openScript((ScriptBase)instance);
						}
					}
				});
			}
			
			// The tree node to be added (Or it may already be there)
			final TreeItem<Instance> newTreeItem = treeItem;
			
			// Get Parent node
			TreeBase<Instance> parentTreeItem = instanceToTreeItemMap.get(instance.getParent());
			if ( parentTreeItem == null )
				parentTreeItem = tree;
			
			// Add to parent
			parentTreeItem.getItems().add(newTreeItem);
			
			// Sort?
			if ( parentTreeItem instanceof SortedTreeItem ) {
				((SortedTreeItem<Instance>)parentTreeItem).sort();
			}
			
			
			// Add connections
			instanceToTreeItemMap.put(instance, newTreeItem);
			treeItemToParentTreeItemMap.put(newTreeItem, parentTreeItem);
			
			LuaConnection changedConnection = instance.changedEvent().connect((args)->{
				LuaValue key = args[0];
				LuaValue val = args[1];
				
				if ( key.eq_b(C_NAME) ) {
					newTreeItem.setText(val.toString());
				}
				
				if ( key.eq_b(C_PARENT) ) {
					destroyNode(instance);
					buildNode(instance);
				}
			});
			treeItemToChangedConnectionMap.put(newTreeItem, changedConnection);
		}
	}
	
	private ContextMenu getContetxMenu(Instance inst) {
		ContextMenu c = new ContextMenu();
		c.setAutoHide(false);

		// Cut
		MenuItem cut = new MenuItem("Cut", Icons.icon_cut.getView());
		cut.setOnAction(event -> {
			List<Instance> instances = Game.getRootInstances(Game.selected());
			Game.copy(instances);
			
			// History snapshot for deleting
			HistorySnapshot snapshot = new HistorySnapshot();
			{
				for (int j = 0; j < instances.size(); j++) {
					Instance root = instances.get(j);
					if ( !root.isInstanceable() ) {
						instances.remove(j--);
						continue;
					}
					List<Instance> desc = root.getDescendants();
					desc.add(0, root);
					for (int i = 0; i < desc.size(); i++) {
						Instance tempInstance = desc.get(i);
						
						HistoryChange change = new HistoryChange(
								Game.historyService().getHistoryStack().getObjectReference(tempInstance),
								LuaValue.valueOf("Parent"),
								tempInstance.getParent(),
								LuaValue.NIL
						);
						snapshot.changes.add(change);
					}
				}
			}
			Game.historyService().pushChange(snapshot);
			
			// Destroy parent object
			for (int i = 0; i < instances.size(); i++) {
				Game.deselect(instances.get(i));
				instances.get(i).destroy();
			}
		});
		c.getItems().add(cut);

		// Copy
		MenuItem copy = new MenuItem("Copy", Icons.icon_copy.getView());
		copy.setOnAction(event -> {
			if ( inst.isInstanceable() ) {
				Game.copy(Game.selected());
			}
		});
		c.getItems().add(copy);

		// Paste
		MenuItem paste = new MenuItem("Paste", Icons.icon_paste.getView());
		paste.setOnAction(event -> {
			List<Instance> instances = Game.paste(inst);
			
			// History snapshot for deleting
			HistorySnapshot snapshot = new HistorySnapshot();
			{
				for (int j = 0; j < instances.size(); j++) {
					Instance root = instances.get(j);
					List<Instance> desc = root.getDescendants();
					desc.add(0, root);
					for (int i = 0; i < desc.size(); i++) {
						Instance tempInstance = desc.get(i);
						
						HistoryChange change = new HistoryChange(
								Game.historyService().getHistoryStack().getObjectReference(tempInstance),
								LuaValue.valueOf("Parent"),
								LuaValue.NIL,
								tempInstance.getParent()
						);
						snapshot.changes.add(change);
					}
				}
			}
			Game.historyService().pushChange(snapshot);
		});
		c.getItems().add(paste);

		// Copy
		MenuItem duplicate = new MenuItem("Duplicate", Icons.icon_copy.getView());
		duplicate.setOnAction(event -> {
			List<Instance> instances = Game.getRootInstances(Game.selected());
			
			// History snapshot for duplicating
			HistorySnapshot snapshot = new HistorySnapshot();
			{
				for (int j = 0; j < instances.size(); j++) {
					Instance root = instances.get(j);
					if ( !root.isInstanceable() ) {
						instances.remove(j--);
						continue;
					}

					// Clone the root instance
					Instance t = root.clone();
					if ( t == null || t.isnil() )
						continue;
					t.forceSetParent(root.getParent());
					
					// Add snapshot change for root clone & all descendents
					List<Instance> desc = t.getDescendants();
					desc.add(0, t);
					for (int i = 0; i < desc.size(); i++) {
						Instance tempInstance = desc.get(i);
						
						HistoryChange change = new HistoryChange(
								Game.historyService().getHistoryStack().getObjectReference(tempInstance),
								LuaValue.valueOf("Parent"),
								LuaValue.NIL,
								tempInstance.getParent()
						);
						snapshot.changes.add(change);
					}
				}
			}
			Game.historyService().pushChange(snapshot);
		});
		c.getItems().add(duplicate);
		
		List<ContextMenuType> menus = ContextMenuType.match(inst);
		if ( menus.size() > 0 )
			c.getItems().add(new SeparatorMenuItem());
		
		for (int i = 0; i < menus.size(); i++) {
			ContextMenuType menu = menus.get(i);
			MenuItem menuItem = new MenuItem(menu.getMenuName(), menu.getMenuGraphic());
			menuItem.setOnAction(event->{
				menu.onClick(inst);
			});
			c.getItems().add(menuItem);
		}
		
		// Add separator
		c.getItems().add(new SeparatorMenuItem());

		// Cut
		MenuItem insert = new MenuItem("Insert Object  \u25ba", Icons.icon_new.getView());
		insert.setOnAction(event -> {
			new InsertWindow();
		});
		c.getItems().add(insert);
		
		return c;
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
