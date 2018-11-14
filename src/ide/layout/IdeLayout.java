package ide.layout;

import engine.Game;
import engine.InternalGameThread;
import engine.io.Load;
import engine.io.Save;
import engine.util.JVMUtil;
import ide.IDE;
import ide.RunnerClient;
import ide.RunnerServer;
import ide.layout.windows.IdeConsole;
import ide.layout.windows.IdeExplorer;
import ide.layout.windows.IdeGameView;
import ide.layout.windows.IdeProperties;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.collections.ObservableList;
import lwjgui.geometry.Orientation;
import lwjgui.geometry.Pos;
import lwjgui.scene.Region;
import lwjgui.scene.control.Menu;
import lwjgui.scene.control.MenuBar;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.SplitPane;
import lwjgui.scene.layout.BorderPane;

public class IdeLayout {
	
	private IdeDockPane south;
	private IdeVerticalDock west;
	private IdeVerticalDock east;
	private IdeDockPane center;
	private IdePane gameView;
	
	public IdeLayout(Region root) {
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
			
			// Create File Menu
			fileMenu(bar);
			
			// Create Edit Menu
			testMenu(bar);
		}
		
		
		// Create split pane
		SplitPane split = new SplitPane();
		split.setFillToParentHeight(true);
		split.setFillToParentWidth(true);
		split.setOrientation(Orientation.VERTICAL);
		pane.setCenter(split);
		
		// Add left
		west = new IdeVerticalDock();
		//west.dockNorth(new IdeExplorer());
		split.getItems().add(west);
		
		// Add middle
		SplitPane middle = middlePane(split);
		
		// Add right
		east = new IdeVerticalDock();
		east.dockNorth(new IdeExplorer());
		east.dockSouth(new IdeProperties());
		split.getItems().add(east);

		// TESTING
		SplitPane.setResizableWithParent(split.getItems().get(0), false);
		SplitPane.setResizableWithParent(split.getItems().get(2), false);
		SplitPane.setResizableWithParent(middle.getItems().get(1), false);
		
		LWJGUI.runLater(() -> {
			split.setDividerPosition(0, 0.0);
			split.setDividerPosition(1, 0.75);
			middle.setDividerPosition(0, 0.8);
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
			gameView = new IdeGameView(IDE.pipeline);
			center.dock(gameView);
			mid.getItems().add(center);
		}
		
		{
			south = new IdeDockPane();
			south.dock(new IdeConsole());
			mid.getItems().add(south);
		}
		
		return mid;
	}
	
	private void testMenu(MenuBar menuBar) {
		Menu menuEdit = new Menu("Test");
		menuBar.getItems().add(menuEdit);
		
		// Normal server test
		MenuItem t2 = new MenuItem("Test Server");
		t2.setGraphic(Icons.icon_play_server.getView());
		t2.setOnAction( event -> {
			if ( Game.isRunning() )
				return;
			
			boolean saved = Save.save();
			if ( !saved )
				return;
			
			JVMUtil.newJVM(RunnerServer.class, new String[] {Game.saveFile});
		});
		menuEdit.getItems().add(t2);
		
		// Normal test
		MenuItem t = new MenuItem("Test Client");
		t.setGraphic(Icons.icon_play.getView());
		t.setOnAction( event -> {
			if ( Game.isRunning() )
				return;
			
			boolean saved = Save.save();
			if ( !saved )
				return;
			
			JVMUtil.newJVM(RunnerClient.class, new String[] {Game.saveFile});
		});
		menuEdit.getItems().add(t);
		
		// Test inside IDE (buggy)
		/*MenuItem t2 = new MenuItem("Test Internal (buggy)");
		t2.setGraphic(Icons.icon_play.getView());
		t2.setOnAction( event -> {
			boolean saved = Save.save();
			if ( !saved )
				return;
			
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
				
				// End Test
				Game.internalTesting = false;
				Game.setRunning(false);
				Load.load(Game.saveFile);
				InternalGameThread.runLater(()->{
					Game.setRunning(false);
				});
				items.clear();
			});
			menuEdit.getItems().add(endTest);
		});
		menuEdit.getItems().add(t2);*/
		
		// Normal server test
		MenuItem ts = new MenuItem("Test Server (IDE)");
		ts.setGraphic(Icons.icon_play_server.getView());
		ts.setOnAction( event -> {
			if ( Game.isRunning() )
				return;
			
			boolean saved = Save.save();
			if ( !saved )
				return;
			
			JVMUtil.newJVM(IDE.class, new String[] {"server",Game.saveFile});
		});
		menuEdit.getItems().add(ts);
		
		
		// Normal client test
		MenuItem tc = new MenuItem("Test Client (IDE)");
		tc.setGraphic(Icons.icon_play.getView());
		tc.setOnAction( event -> {
			if ( Game.isRunning() )
				return;
			
			boolean saved = Save.save();
			if ( !saved )
				return;
			
			JVMUtil.newJVM(IDE.class, new String[] {"client",Game.saveFile});
		});
		menuEdit.getItems().add(tc);
	}

	private void fileMenu(MenuBar menuBar) {
		Menu menuFile = new Menu("File");
		
		MenuItem mnew = new MenuItem("New");
		mnew.setGraphic(Icons.icon_new.getView());
		mnew.setOnAction(event -> {
			Game.newProject();
		});
		menuFile.getItems().add(mnew);
		//mnew.setAccelerator(KeyCombination.valueOf("SHORTCUT+N"));
		
		MenuItem mload = new MenuItem("Open");
		mload.setGraphic(Icons.icon_folder.getView());
		mload.setOnAction(event -> {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					Load.load();
				}
			};
			if ( Game.changes ) {
				Save.requestSave(r);
			} else {
				r.run();
			}
		});
		menuFile.getItems().add(mload);
		//mload.setAccelerator(KeyCombination.valueOf("SHORTCUT+O"));
		
		MenuItem msave = new MenuItem("Save");
		msave.setGraphic(Icons.icon_save.getView());
		msave.setOnAction(event -> {
			Save.save();
		});
		menuFile.getItems().add(msave);
		//msave.setAccelerator(KeyCombination.valueOf("SHORTCUT+S"));

		MenuItem msave2 = new MenuItem("Save As...");
		msave2.setGraphic(Icons.icon_saveas.getView());
		msave2.setOnAction(event -> {
			Save.save(true);
		});
		menuFile.getItems().add(msave2);
		//msave2.setAccelerator(KeyCombination.valueOf("SHORTCUT+SHIFT+S"));
		
		MenuItem mquit = new MenuItem("Quit");
		mquit.setGraphic(Icons.icon_cross.getView());
		menuFile.getItems().add(mquit);
		mquit.setOnAction(event -> {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					System.exit(0);
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
