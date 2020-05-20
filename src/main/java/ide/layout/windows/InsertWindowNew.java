package ide.layout.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalRenderThread;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.geometry.Insets;
import lwjgui.scene.Context;
import lwjgui.scene.Node;
import lwjgui.scene.Scene;
import lwjgui.scene.control.ContextMenu;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.control.SearchField;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.VBox;
import lwjgui.style.BorderStyle;
import lwjgui.style.BoxShadow;
import lwjgui.theme.Theme;

public class InsertWindowNew extends ContextMenu {
	private static final ArrayList<Class<? extends Instance>> TYPES;
	
	static {
		
		// Get instanceable classes
		TYPES = Instance.getInstanceableTypes();
		
		// Sort
		Collections.sort(TYPES, new Comparator<Class<? extends Instance>>() {
			@Override
			public int compare(Class<? extends Instance> o1, Class<? extends Instance> o2) {
				int priority1 = IdeExplorer.getPriority(o1);
				int priority2 = IdeExplorer.getPriority(o2);
				
				if ( priority1 == priority2 )
					return 0;
				
				if ( priority1 < priority2 )
					return 1;
				
				return -1;
			}
		});
	}

	private SearchField input;
	
	private VBox itemsPane;
	
	private Instance parentInst;
	
	public InsertWindowNew(Instance parentInst) {
		this.parentInst = parentInst;
		
		this.setPadding(Insets.EMPTY);
		
		VBox mainBox = new VBox();
		mainBox.setPrefWidth(200);
		mainBox.setFillToParentHeight(true);
		this.getChildren().add(mainBox);
		
		// Search bar
		{
			StackPane pane = new StackPane();
			pane.setBackgroundLegacy(Theme.current().getPaneAlt());
			pane.setFillToParentWidth(true);
			pane.setPadding(new Insets(8));
			mainBox.getChildren().add(pane);
			
			pane.getBoxShadowList().add(new BoxShadow(0, 2, 5, -1, Theme.current().getShadow()));
			
			input = new SearchField();
			input.setFillToParentWidth(true);
			input.setPrompt("Search object");
			input.setOnTextChange((event)->{
				updateItems();
			});
			pane.getChildren().add(input);
		}
		
		// Items
		{
			ScrollPane pane = new ScrollPane();
			pane.setPrefHeight(256);
			pane.setFillToParentWidth(true);
			pane.setBorderStyle(BorderStyle.NONE);
			mainBox.getChildren().add(pane);
			
			itemsPane = new VBox();
			itemsPane.setPadding(new Insets(6));
			itemsPane.setSpacing(6);
			pane.setContent(itemsPane);
			
			updateItems();
		}
	}
	
	private void updateItems() {
		itemsPane.getChildren().clear();
		
		String searchString = input.getText();
		boolean all = searchString == null || searchString.length() == 0;
		
		// Display
		for (int i = 0; i< TYPES.size(); i++) {
			try {
				Class<?> instClass = TYPES.get(i);
				
				String instName = instClass.getSimpleName().toString().toLowerCase();
				if ( !all && !instName.contains(searchString.toLowerCase()))
					continue;
				
				Instance temp = (Instance) instClass.newInstance();
				
				Node icon = Icons.icon_wat.getView();
				if ( temp instanceof TreeViewable ) {
					icon = ((TreeViewable)temp).getIcon().getView();
				}
				
				Label t = new Label(temp.getName()) {
					@Override
					public void render(Context context) {
						super.render(context);
					}
				};
				t.setGraphic(icon);
				icon.setMouseTransparent(true);
				itemsPane.getChildren().add(t);
				
				t.setOnMouseClicked((event)->{
					if ( event.getClickCount() == 2 ) {
						InternalRenderThread.runLater(()->{
							try {
								Instance inst = Instance.instanceLua(temp.getClassName().toString());
								inst.forceSetParent(parentInst);
								Game.historyService().pushChange(inst, LuaValue.valueOf("Parent"), LuaValue.NIL, inst.getParent());
								
								//Game.select(inst);
							} catch (Exception e) {
								//
							}
						});
						this.close();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void show(Scene scene, double x, double y) {
		super.show(scene, x, y);
		
		LWJGUI.runLater(()->{
			this.window.getContext().setSelected(input);
		});
	}
}
