/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout;

import org.json.simple.JSONObject;
import org.luaj.vm2.LuaValue;

import engine.ClientEngine;
import engine.ClientLocalRunner;
import engine.ClientServerRunner;
import engine.Game;
import engine.InternalGameThread;
import engine.io.Load;
import engine.io.Save;
import engine.lua.type.object.services.Core;
import engine.util.JVMUtil;
import ide.IDE;
import ide.layout.windows.IdeConsole;
import ide.layout.windows.IdeExplorer;
import ide.layout.windows.IdeGameView;
import ide.layout.windows.IdeMaterialViewer;
import ide.layout.windows.IdeProperties;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.collections.ObservableList;
import lwjgui.geometry.Orientation;
import lwjgui.geometry.Pos;
import lwjgui.scene.Group;
import lwjgui.scene.control.Menu;
import lwjgui.scene.control.MenuBar;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.SeparatorMenuItem;
import lwjgui.scene.control.SplitPane;
import lwjgui.scene.layout.BorderPane;

public class IdeLayout {
	
	private IdeDockPane south;
	private IdeVerticalDock west;
	private IdeVerticalDock east;
	private IdeDockPane center;
	public IdeGameView gameView;
	
	public IdeLayout(Group root) {
		root.setAlignment(Pos.TOP_LEFT);
	
		BorderPane pane = new BorderPane();
		pane.setFillToParentHeight(true);
		pane.setFillToParentWidth(true);
		root.getChildren().add(pane);
		
		// Top part of borderpane
		{
			// Create Menu Bar
			MenuBar bar = new MenuBar();
			pane.setTop(bar);
			
			// Create menus
			fileMenu(bar);
			editMenu(bar);
			testMenu(bar);
		}
		
		
		// Create split pane
		SplitPane split = new SplitPane();
		split.setFillToParentHeight(true);
		split.setFillToParentWidth(true);
		split.setOrientation(Orientation.VERTICAL);
		pane.setCenter(split);
		
		// Bottom bar
		{
			MenuBar bar = new MenuBar();
			pane.setBottom(bar);
		}
		
		// Add left
		west = new IdeVerticalDock();
		west.dockNorth(new IdeExplorer(Game.project()));
		split.getItems().add(west);
		
		// Add middle
		SplitPane middle = middlePane(split);
		
		// Add right
		east = new IdeVerticalDock();
		east.dockNorth(new IdeExplorer(Game.game()));
		east.dockSouth(new IdeProperties());
		split.getItems().add(east);
		
		// Dock south
		south.dock(new IdeConsole());
		south.dock(new IdeMaterialViewer());
		
		LWJGUI.runLater(() -> {
			south.select(south.getTabs().get(0));
			
			split.setDividerPosition(0, 0.2);
			split.setDividerPosition(1, 0.75);
			middle.setDividerPosition(0, 0.725);
			
			SplitPane.setResizableWithParent(split.getItems().get(0), false);
			SplitPane.setResizableWithParent(split.getItems().get(2), false);
			SplitPane.setResizableWithParent(middle.getItems().get(1), false);
		});	
	}
	
	private SplitPane middlePane(SplitPane split) {
		SplitPane mid = new SplitPane();
		mid.setFillToParentHeight(true);
		mid.setFillToParentWidth(true);
		mid.setOrientation(Orientation.HORIZONTAL);
		split.getItems().add(mid);
		
		
		{
			center = new IdeDockPane();
			gameView = new IdeGameView(ClientEngine.renderThread.getPipeline());
			center.dock(gameView);
			mid.getItems().add(center);
		}
		
		{
			south = new IdeDockPane();
			mid.getItems().add(south);
		}
		
		return mid;
	}
	
	private void editMenu(MenuBar menuBar) {
		Menu menuEdit = new Menu("Edit");
		menuEdit.setAutoHide(false);
		menuBar.getItems().add(menuEdit);
		
		MenuItem undo = new MenuItem("Undo", Icons.icon_undo.getView());
		undo.setOnAction( event -> {
			Game.historyService().undo();
		});
		menuEdit.getItems().add(undo);
		
		
		MenuItem redo = new MenuItem("Redo", Icons.icon_redo.getView());
		redo.setOnAction( event -> {
			Game.historyService().redo();
		});
		menuEdit.getItems().add(redo);
		
		
		MenuItem unselect = new MenuItem("Unselect All");
		unselect.setOnAction( event -> {
			Game.deselectAll();
		});
		menuEdit.getItems().add(unselect);
	}
	
	private void testMenu(MenuBar menuBar) {
		Menu menuEdit = new Menu("Test");
		menuEdit.setAutoHide(false);
		menuBar.getItems().add(menuEdit);
		
		// Normal server test
		MenuItem t2 = new MenuItem("Test Server", Icons.icon_play_server.getView());
		t2.setOnAction( event -> {
			if ( Game.isRunning() )
				return;
			
			boolean saved = Save.save();
			if ( !saved )
				return;
			
			JVMUtil.newJVM(ClientServerRunner.class, new String[] {Game.saveFile});
		});
		menuEdit.getItems().add(t2);
		

		// Normal server test
		MenuItem ts = new MenuItem("Test Server (IDE)", Icons.icon_play_server.getView());
		ts.setOnAction( event -> {
			if ( Game.isRunning() )
				return;
			
			boolean saved = Save.save();
			if ( !saved )
				return;
			
			JVMUtil.newJVM(IDE.class, new String[] {"server",Game.saveFile});
		});
		menuEdit.getItems().add(ts);
		
		// Separator!
		menuEdit.getItems().add(new SeparatorMenuItem());
		
		// Normal test
		MenuItem t = new MenuItem("Test Client", Icons.icon_play.getView());
		t.setOnAction( event -> {
			if ( Game.isRunning() )
				return;
			
			boolean saved = Save.save();
			if ( !saved )
				return;
			
			JVMUtil.newJVM(ClientLocalRunner.class, new String[] {Game.saveFile});
		});
		menuEdit.getItems().add(t);
		
		// Normal client test
		MenuItem tc = new MenuItem("Test Client (IDE)", Icons.icon_play.getView());
		tc.setOnAction( event -> {
			if ( Game.isRunning() )
				return;
			
			boolean saved = Save.save();
			if ( !saved )
				return;
			
			JVMUtil.newJVM(IDE.class, new String[] {"client",Game.saveFile});
		});
		menuEdit.getItems().add(tc);
		
		// Separator!
		menuEdit.getItems().add(new SeparatorMenuItem());
		
		// Test inside IDE (buggy)
		MenuItem internalTest = new MenuItem("Test Internal (buggy)", Icons.icon_play.getView());
		internalTest.setOnAction( event -> {
			JSONObject projectJson = Save.getProjectJSON(false);
			JSONObject gameJson = Save.getGameJSON();
			Game.internalTesting = true;
			Game.setRunning(true);
			
			// Clear default items
			ObservableList<MenuItem> items = new ObservableList<MenuItem>(menuEdit.getItems());
			menuEdit.getItems().clear();
			
			// Add end button
			MenuItem endTest = new MenuItem("END TEST");
			endTest.setOnAction( ee -> {
				menuEdit.getItems().clear();
				for (int i = 0; i < items.size(); i++) {
					menuEdit.getItems().add(items.get(i));
				}
				try {
					// End Test
					Game.internalTesting = false;
					Game.setRunning(false);

					// Reload from stored JSON
					InternalGameThread.runLater(()->{
						// Reload project
						//Game.unload();
						Game.setLoaded(false);
						
						// Reload project
						Load.parseJSONInto(projectJson, Game.project(), true);
						
						// Reload game
						Load.parseJSONInto(gameJson, Game.game(), true);
						
						// Load game
						Game.load();
						
						// Reset core
						/*Core core = Game.core();
						if ( core != null ) {
							core.forceSetParent(LuaValue.NIL);
							core.forceSetParent(Game.game());
						}*/
					});
				} catch(Exception e) {
					e.printStackTrace();
				}
				items.clear();
			});
			menuEdit.getItems().add(endTest);
		});
		menuEdit.getItems().add(internalTest);
	}

	private void fileMenu(MenuBar menuBar) {
		Menu menuFile = new Menu("File");
		menuFile.setAutoHide(false);
		
		MenuItem mnew = new MenuItem("New", Icons.icon_new.getView());
		mnew.setOnAction(event -> {
			Game.newProject();
		});
		menuFile.getItems().add(mnew);
		//mnew.setAccelerator(KeyCombination.valueOf("SHORTCUT+N"));
		
		MenuItem mload = new MenuItem("Open", Icons.icon_folder.getView());
		mload.setOnAction(event -> {
			Load.loadSafe();
		});
		menuFile.getItems().add(mload);
		//mload.setAccelerator(KeyCombination.valueOf("SHORTCUT+O"));
		
		MenuItem msave = new MenuItem("Save", Icons.icon_save.getView());
		msave.setOnAction(event -> {
			Save.saveSafe();
		});
		menuFile.getItems().add(msave);
		//msave.setAccelerator(KeyCombination.valueOf("SHORTCUT+S"));

		MenuItem msave2 = new MenuItem("Save As...", Icons.icon_saveas.getView());
		msave2.setOnAction(event -> {
			Save.saveAsSafe();
		});
		menuFile.getItems().add(msave2);
		//msave2.setAccelerator(KeyCombination.valueOf("SHORTCUT+SHIFT+S"));
		
		MenuItem mquit = new MenuItem("Quit", Icons.icon_cross.getView());
		menuFile.getItems().add(mquit);
		mquit.setOnAction(event -> {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					ClientEngine.stop();
				}
			};
			if ( Game.changes ) {
				Save.requestSave(r);
			} else {
				r.run();
			}
		});
		
		menuBar.getItems().add(menuFile);
	}
	
	public void update() {
		west.update();
		east.update();
	}
	
	public IdeDockPane getCenter() {
		return center;
	}

	public IdePane getGamePane() {
		return this.gameView;
	}
}
