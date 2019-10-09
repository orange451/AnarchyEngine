package ide.layout.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.LWJGUIUtil;
import lwjgui.geometry.Insets;
import lwjgui.scene.Context;
import lwjgui.scene.Node;
import lwjgui.scene.Scene;
import lwjgui.scene.Window;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.VBox;
import lwjgui.theme.Theme;

public class InsertWindow {
	
	public InsertWindow() {
		long handle = LWJGUIUtil.createOpenGLCoreWindow("Insert Object", 2, 2, false, true);
		Window window = LWJGUI.initialize(handle);
		
		StackPane root = new StackPane();
		root.setBackground(Theme.current().getControl());
		root.setPadding(new Insets(8));
		
		VBox tempV = new VBox();
		tempV.setBackground(null);
		tempV.setFillToParentHeight(true);
		tempV.setFillToParentWidth(true);
		tempV.setSpacing(4);
		root.getChildren().add(tempV);
		
		Label l = new Label() {
			@Override
			public void position(Node parent) {
				super.position(parent);
				this.setText("Insert object into: " + InsertWindow.this.getParent().getFullName());
			}
		};
		tempV.getChildren().add(l);
		
		ScrollPane scroll = new ScrollPane();
		scroll.setFillToParentHeight(true);
		scroll.setFillToParentWidth(true);
		tempV.getChildren().add(scroll);
		
		VBox items = new VBox();
		scroll.setContent(items);
		
		// Get instanceable classes
		ArrayList<Class<? extends Instance>> TYPES = Instance.getInstanceableTypes();
		
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
		
		// Display
		for (int i = 0; i< TYPES.size(); i++) {
			try {
				Class<?> instClass = TYPES.get(i);
				Instance temp = (Instance) instClass.newInstance();
				
				Node icon = Icons.icon_wat.getView();
				if ( temp instanceof TreeViewable ) {
					icon = ((TreeViewable)temp).getIcon().getView();
				}
				
				Label t = new Label(temp.getName()) {
					@Override
					public void render(Context context) {
						//this.setBackground(context.isSelected(this)?Theme.current().getSelection():null);
						super.render(context);
					}
				};
				t.setGraphic(icon);
				icon.setMouseTransparent(true);
				items.getChildren().add(t);
				
				t.setOnMouseClicked((event)->{
					try {
						if ( event.getClickCount() == 2 ) {
							Instance inst = (Instance) instClass.newInstance();
							inst.forceSetParent(getParent());
							Game.historyService().pushChange(inst, LuaValue.valueOf("Parent"), LuaValue.NIL, inst.getParent());
							
							//Game.select(inst);
						}
					} catch (Exception e) {
						//
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		window.setScene(new Scene( root, 300, 250 ));
		window.show();
	}

	private Instance getParent() {
		List<Instance> selected = Game.selected();
		if ( selected.size() == 0 )
			return Game.workspace();
		
		return selected.get(0);
	}
}
