/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout.windows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.LuaConnection;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.AssetFolder;
import engine.lua.type.object.insts.AudioPlayer2D;
import engine.lua.type.object.insts.AudioPlayer3D;
import engine.lua.type.object.insts.AudioSource;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.Folder;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.PhysicsObject;
import engine.lua.type.object.insts.PlayerPhysics;
import engine.lua.type.object.insts.Scene;
import engine.lua.type.object.insts.animation.AnimationController;
import engine.lua.type.object.insts.animation.AnimationData;
import engine.lua.type.object.insts.light.DirectionalLight;
import engine.lua.type.object.insts.light.PointLight;
import engine.lua.type.object.insts.light.SpotLight;
import engine.lua.type.object.insts.ui.CSS;
import engine.lua.type.object.insts.ui.Gui;
import engine.lua.type.object.insts.ui.GuiBase;
import engine.lua.type.object.insts.values.ValueBase;
import engine.lua.type.object.services.RenderSettings;
import engine.lua.type.object.services.StarterPlayerScripts;
import engine.tasks.TaskManager;
import ide.IDE;
import ide.layout.IdePane;
import ide.layout.windows.icons.Icons;
import lwjgui.collections.ObservableList;
import lwjgui.geometry.Insets;
import lwjgui.scene.Node;
import lwjgui.scene.control.ContextMenu;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.control.SeparatorMenuItem;
import lwjgui.scene.control.TreeBase;
import lwjgui.scene.control.TreeItem;
import lwjgui.scene.control.TreeNode;
import lwjgui.scene.control.TreeView;

public class IdeExplorer extends IdePane {
	private ScrollPane scroller;
	private SortedTreeView<Instance> tree;

	private static final LuaValue C_NAME = LuaValue.valueOf("Name");
	private static final LuaValue C_PARENT = LuaValue.valueOf("Parent");
	
	private Map<Instance, TreeItem<Instance>> instanceToTreeItemMap;
	private Map<TreeItem<Instance>, TreeBase<Instance>> treeItemToParentTreeItemMap;
	private Map<TreeItem<Instance>, LuaConnection> treeItemToChangedConnectionMap;
	private List<Instance> selectedCache;
	
	private Instance rootECS;
	
	private static HashMap<Class<? extends Instance>, Integer> priority = new HashMap<>();
	
	static {		
		priority.put(Camera.class, 50);

		priority.put(StarterPlayerScripts.class, 41);
		priority.put(AssetFolder.class, 41);
		priority.put(RenderSettings.class, 41);
		priority.put(AnimationData.class, 40);
		
		priority.put(ScriptBase.class, 35);

		priority.put(CSS.class, 32);
		priority.put(Gui.class, 31);
		priority.put(GuiBase.class, 30);

		priority.put(DirectionalLight.class, 25);
		priority.put(PointLight.class, 24);
		priority.put(SpotLight.class, 23);
		
		priority.put(Folder.class, 20);

		priority.put(GameObject.class, 8);
		priority.put(AnimationController.class, 7);
		priority.put(PhysicsBase.class, 6);
		priority.put(PhysicsObject.class, 6);
		priority.put(PlayerPhysics.class, 6);

		priority.put(ValueBase.class, 5);

		priority.put(AudioSource.class, 4);
		priority.put(AudioPlayer2D.class, 4);
		priority.put(AudioPlayer3D.class, 4);
	}
	
	protected static int getPriority(Class<? extends Instance> cls) {
		Integer ret = priority.get(cls);
		if ( ret == null ) {
			Iterator<Entry<Class<? extends Instance>, Integer>> iterator = priority.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<Class<? extends Instance>, Integer> entry = iterator.next();
				Class<? extends Instance> testClaz = entry.getKey();
				int p = entry.getValue();
				
				if ( testClaz.isAssignableFrom(cls) ) {
					System.out.println("\t\tADDING NEW PRIORITY: " + testClaz + " / " + p);
					priority.put(testClaz, p);
					ret = p;
				}
			}
		}
		if ( ret == null )
			return 0;
		
		return ret;
	}
	
	public IdeExplorer(Instance root) {
		super("Explorer", true);
		
		this.rootECS = root;

		this.scroller = new ScrollPane();
		this.scroller.setBorder(Insets.EMPTY);
		this.scroller.setFillToParentHeight(true);
		this.scroller.setFillToParentWidth(true);
		this.getChildren().add(scroller);
		
		// Object Added
		root.descendantAddedEvent().connect((args)->{
			if ( !Game.isLoaded() )
				return;
			TaskManager.addTaskRenderThread(() -> buildNode((Instance) args[0], true));
		});
		
		// Object removed
		root.descendantRemovedEvent().connect((args)->{
			TaskManager.addTaskRenderThread(() -> destroyNode((Instance) args[0]));
		});
		
		// Game reset
		Game.resetEvent().connect((args) -> {
			TaskManager.addTaskRenderThread(() -> {
				rebuild(root);
			});
		});
		
		// First (initial) load
		TaskManager.addTaskRenderThread(() -> {
			rebuild(root);
		});
		
		// Selection change
		Game.selectionChanged().connect((args)->{
			List<Instance> selected = Game.selected();
			List<Instance> toSelect = new ArrayList<Instance>();
			List<Instance> toUnselect = new ArrayList<Instance>();
			
			// Prune left (unselect those that were previously selected but no longer selected)
			for (int i = 0; i < selectedCache.size(); i++) {
				Instance alreadySelected = selectedCache.get(i);
				if ( !selected.contains(alreadySelected) )
					toUnselect.add(alreadySelected);
			}
			
			// Prune right (Select those that were NOT previously selected but are now).
			for (int i = 0; i < selected.size(); i++) {
				Instance potentialNotYetSelected = selected.get(i);
				if ( !selectedCache.contains(potentialNotYetSelected) )
					toSelect.add(potentialNotYetSelected);
			}
			
			// Unselect old ones
			for (int i = 0; i < toUnselect.size(); i++) {
				Instance t = toUnselect.get(i);
				TreeItem<Instance> t2 = instanceToTreeItemMap.get(t);
				if ( t2 == null )
					continue;
				
				tree.deselectItem(t2);
			}
			
			// select new ones
			for (int i = 0; i < toSelect.size(); i++) {
				Instance t = toSelect.get(i);
				TreeItem<Instance> t2 = instanceToTreeItemMap.get(t);
				if ( t2 == null )
					continue;
				
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
		this.instanceToTreeItemMap = Collections.synchronizedMap(new HashMap<>());
		
		if ( treeItemToParentTreeItemMap != null )
			treeItemToParentTreeItemMap.clear();
		this.treeItemToParentTreeItemMap = Collections.synchronizedMap(new HashMap<>());
		
		if ( treeItemToChangedConnectionMap != null )
			treeItemToChangedConnectionMap.clear();
		this.treeItemToChangedConnectionMap = Collections.synchronizedMap(new HashMap<>());
		
		if ( selectedCache != null )
			selectedCache.clear();
		this.selectedCache = new ArrayList<Instance>();

		this.tree = new SortedTreeView<Instance>() {
			@Override
			public void resize() {
				super.resize();
				this.setPrefWidth(scroller.getViewportWidth());
			}
		};
		this.scroller.setContent(tree);
		
		tree.setOnSelectItem(event -> {
			TreeItem<Instance> item = event.object;
			Instance inst = item.getRoot();
			
			if ( Game.selected().size() > 0 ) {
				if ( !Game.selected().get(Game.selected().size()-1).isDescendantOf(rootECS) ) {
					Game.deselectAll();
				}
			}
			
			Game.select(inst);
		});
		tree.setOnDeselectItem(event -> {
			TreeItem<Instance> item = event.object;
			Instance inst = item.getRoot();
			Game.deselect(inst);
		});
	}
	
	private void rebuild(Instance root) {
		clear();
		
		System.out.println("REBUILDING EXPLORER TREE FOR ROOT OBJECT: " + root + " / " + Arrays.toString(root.getDescendants().toArray()));
		
		buildDescendents(root, true);
	}
	
	private void buildDescendents(Instance parent, boolean sort) {
		List<Instance> gameDescendents = parent.getDescendantsUnsafe();
		for (int i = 0; i < gameDescendents.size(); i++) {
			if ( i >= gameDescendents.size() )
				continue;
			
			Instance desc = gameDescendents.get(i);
			if ( desc == null )
				continue;
			
			buildNode(desc, sort);
		}
	}
	
	private void destroyNode(Instance instance) {
		synchronized(instanceToTreeItemMap) {
			TreeItem<Instance> treeItem = instanceToTreeItemMap.get(instance);
			if ( treeItem == null )
				return;
			
			TreeBase<Instance> parentTreeItem = treeItemToParentTreeItemMap.get(treeItem);
			if ( parentTreeItem == null )
				return;
			
			while(parentTreeItem.getItems().contains(treeItem))
				parentTreeItem.getItems().remove(treeItem);
			
			instanceToTreeItemMap.remove(instance);
			treeItemToParentTreeItemMap.remove(treeItem);
			
			LuaConnection con = treeItemToChangedConnectionMap.get(treeItem);
			if ( con != null )
				con.disconnect();
			treeItemToChangedConnectionMap.remove(treeItem);
		}
	}
	
	private synchronized void buildNode(Instance instance, boolean sort) {
		if ( instance.getParent().isnil() )
			return;
		
		if ( instance.isDestroyed() )
			return;
		
		synchronized(instanceToTreeItemMap) {
			// Get Parent node
			LuaValue parInst = instance.getParent();
			if ( parInst.isnil() )
				parInst = rootECS;
			TreeBase<Instance> parentTreeItem = instanceToTreeItemMap.get(parInst);
			if ( parInst.isnil() || parInst.eq_b(rootECS) )
				parentTreeItem = tree;
			if ( parentTreeItem == null )
				return;
			
			// Get grandparent node
			LuaValue grandparInst = rootECS;
			if ( parentTreeItem instanceof TreeItem )
				grandparInst = ((TreeItem<Instance>)parentTreeItem).getRoot().getParent();
			TreeBase<Instance> grandparentTreeItem = instanceToTreeItemMap.get(grandparInst);
			if ( grandparentTreeItem == null )
				grandparentTreeItem = tree;
			
			// Get Tree Node
			TreeItem<Instance> treeItem = instanceToTreeItemMap.get(instance);
			if ( treeItem == null ) {
				// Parent (or grandparent) MUST be open for children to be created!
				if ( !parentTreeItem.isExpanded() && !grandparentTreeItem.isExpanded() )
					return;
				
				// If the grandparent has at least 1 item in it
				if ( !parentTreeItem.isExpanded() && parentTreeItem.getItems().size() > 0 )
					return;
				
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
						if ( instance instanceof CSS ) {
							IDE.openCSS((CSS)instance);
						}
						if ( instance instanceof Scene ) {
							Game.game().loadScene((Scene)instance);
						}
					}
				});
			}
			
			// The tree node to be added (Or it may already be there)
			final TreeItem<Instance> newTreeItem = treeItem;
			
			// Add to parent
			synchronized(parentTreeItem.getItems()) {
				if ( !parentTreeItem.getItems().contains(newTreeItem) ) {
					parentTreeItem.getItems().add(newTreeItem);

					// Sort?
					if ( parentTreeItem instanceof SortedTreeItem ) {
						((SortedTreeItem<Instance>)parentTreeItem).sort();
					}
					
					// Add connections
					instanceToTreeItemMap.put(instance, newTreeItem);
					treeItemToParentTreeItemMap.put(newTreeItem, parentTreeItem);
					
					// Track object changes
					LuaConnection changedConnection = instance.changedEvent().connect((args)->{
						LuaValue key = args[0];
						LuaValue val = args[1];
						
						if ( key.eq_b(C_NAME) ) {
							newTreeItem.setText(val.toString());
						}
						
						if ( key.eq_b(C_PARENT) ) {
							destroyNode(instance);
							if ( !val.isnil() ) {
								buildNode(instance, true);
								buildDescendents(instance, true);
							}
							tree.refresh();
						}
					});
					treeItemToChangedConnectionMap.put(newTreeItem, changedConnection);
				}
			}
		}
	}
	
	private ContextMenu getContetxMenu(Instance inst) {
		ContextMenu c = new ContextMenu();
		c.setAutoHide(false);

		// Cut
		MenuItem cut = new MenuItem("Cut", Icons.icon_cut.getView());
		cut.setOnAction(event -> {
			Game.cutSelection();
		});
		c.getItems().add(cut);

		// Copy
		MenuItem copy = new MenuItem("Copy", Icons.icon_copy.getView());
		copy.setOnAction(event -> {
			Game.copySelection();
		});
		c.getItems().add(copy);

		// Paste
		MenuItem paste = new MenuItem("Paste", Icons.icon_paste.getView());
		paste.setOnAction(event -> {
			Game.paste(inst);
		});
		c.getItems().add(paste);

		// Copy
		MenuItem duplicate = new MenuItem("Duplicate", Icons.icon_copy.getView());
		duplicate.setOnAction(event -> {
			Game.duplicateSelection();
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
		@Override
		public boolean isExpanded() {
			return true;
		}
		
		public void refresh() {
			this.needsRefresh = true;
		}
	}
	
	class SortedTreeItem<E> extends TreeItem<E> {

		public SortedTreeItem(E root) {
			super(root);
		}
		
		public SortedTreeItem(E root, Node node) {
			super(root, node);
		}
		
		@Override
		public void setExpanded(boolean expanded) {
			if ( this.isExpanded() == expanded )
				return;
			
			super.setExpanded(expanded);
			
			if ( expanded ) {
				 if ( this.getRoot() instanceof Instance )
					buildDescendents((Instance)this.getRoot(), false);
				sort();
			}
		}
		
		protected void sort() {
			if ( !this.isExpanded() )
				return;
			
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
