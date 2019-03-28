package ide.layout.windows;

import java.util.ArrayList;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.LWJGUIUtil;
import lwjgui.geometry.Insets;
import lwjgui.paint.Color;
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
	
	public InsertWindow(Instance parent) {
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
		
		Label l = new Label("Insert object into: " + parent.getFullName());
		tempV.getChildren().add(l);
		
		ScrollPane scroll = new ScrollPane() {
			@Override
			public void position(Node parent) {
				super.position(parent);
				
				System.out.println(this.getContent().getY() + " / " + this.getY() + " / " + tempV.getY());
			}
		};
		scroll.setFillToParentHeight(true);
		scroll.setFillToParentWidth(true);
		tempV.getChildren().add(scroll);
		
		VBox items = new VBox();
		items.setBackground(Color.ORANGE);
		scroll.setContent(items);
		
		ArrayList<Class<?>> TYPES = Instance.getInstanceableTypes();
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
						this.setBackground(context.isSelected(this)?Theme.current().getSelection():null);
						super.render(context);
					}
				};
				t.setGraphic(icon);
				items.getChildren().add(t);
				
				t.setOnMouseClicked((event)->{
					try {
						if ( event.getClickCount() == 2 ) {
							Instance inst = (Instance) instClass.newInstance();
							inst.forceSetParent(parent);
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

}
