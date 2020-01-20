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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.InternalRenderThread;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.LWJGUIUtil;
import lwjgui.ManagedThread;
import lwjgui.geometry.Insets;
import lwjgui.scene.Context;
import lwjgui.scene.Node;
import lwjgui.scene.Scene;
import lwjgui.scene.Window;
import lwjgui.scene.WindowHandle;
import lwjgui.scene.WindowManager;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.VBox;
import lwjgui.theme.Theme;

public class InsertWindow {
	
	public InsertWindow() {
		WindowManager.runLater(() -> {
			ManagedThread thread = new ManagedThread(300, 100, "Save") {
				@Override
				protected void setupHandle(WindowHandle handle) {
					super.setupHandle(handle);
					handle.canResize(false);
				}
				@Override
				protected void init(Window window) {
					super.init(window);

					StackPane root = new StackPane();
					root.setBackgroundLegacy(Theme.current().getControl());
					root.setPadding(new Insets(8));
					
					ScrollPane scroll = new ScrollPane();
					scroll.setFillToParentHeight(true);
					scroll.setFillToParentWidth(true);
					root.getChildren().add(scroll);
					
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
								if ( event.getClickCount() == 2 ) {
									InternalRenderThread.runLater(()->{
										try {
											Instance inst = Instance.instanceLua(temp.getClassName().toString());
											inst.forceSetParent(getParent());
											Game.historyService().pushChange(inst, LuaValue.valueOf("Parent"), LuaValue.NIL, inst.getParent());
										} catch (Exception e) {
											//
										}
									});
									
									//Game.select(inst);
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					window.setTitle("Insert Object: " + InsertWindow.this.getParent().getFullName());
					window.setScene(new Scene( root, 300, 250 ));
					window.show();
				}
			};
			thread.start();
		});
	}

	private Instance getParent() {
		List<Instance> selected = Game.selected();
		if ( selected.size() == 0 )
			return Game.workspace();
		
		return selected.get(0);
	}
}
