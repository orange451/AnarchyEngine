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

import java.util.List;

import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;

import engine.Game;
import engine.io.Save;
import engine.lua.history.HistoryChange;
import engine.lua.history.HistorySnapshot;
import engine.lua.type.object.Instance;
import engine.lua.type.object.insts.Folder;
import lwjgui.LWJGUI;
import lwjgui.scene.Node;
import lwjgui.scene.control.TextInputControl;

public class StandardUserControls {

	public static void bind(Node object) {
		object.setOnKeyPressed((event)->{
			
			// Node must be selected
			//if ( !LWJGUI.getCurrentContext().isSelected(object) && !object.isDescendentSelected() )
			//	return;
			
			// Delete Selection
			if ( event.getKey() == GLFW.GLFW_KEY_DELETE ) {
				if ( object.getWindow().getContext().getSelected() instanceof TextInputControl )
					return;
				
				Game.deleteSelection();
			}
			
			// Group objects
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_G ) {
				if ( object.getWindow().getContext().getSelected() instanceof TextInputControl )
					return;
				
				List<Instance> selected = Game.selected();
				List<Instance> root = Game.getRootInstances(selected);
				if ( root.size() == 0 )
					return;
				
				Instance group = new Folder();
				group.setParent(root.get(0).getParent());
				
				if ( group.getParent().isnil() ) {
					group.destroy();
					return;
				}
				
				HistorySnapshot snapshot = new HistorySnapshot();
				{
					HistoryChange groupCreation = new HistoryChange(Game.historyService().getHistoryStack(), group, LuaValue.valueOf("Parent"), LuaValue.NIL, group.getParent());
					snapshot.addChange(groupCreation);
					
					for (int i = 0; i < root.size(); i++) {
						Instance obj = root.get(i);
						LuaValue oldParent = obj.getParent();
						obj.setParent(group);
						
						// If we reparented it successfully, add to history
						if ( obj.getParent().eq_b(group) ) {
							HistoryChange c = new HistoryChange(Game.historyService().getHistoryStack(), obj, LuaValue.valueOf("Parent"), oldParent, group );
							snapshot.addChange(c);
						}
					}
				}
				
				if ( group.getChildren().size() == 0 )
					group.destroy();
				else
					Game.historyService().pushChange(snapshot);
			}
			
			// Duplicate object
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_D ) {
				if ( object.getWindow().getContext().getSelected() instanceof TextInputControl )
					return;
				
				Game.duplicateSelection();
			}
			
			// Cut object
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_X ) {
				if ( object.getWindow().getContext().getSelected() instanceof TextInputControl )
					return;
				
				Game.cutSelection();
			}
			
			// Copy object
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_C ) {
				if ( object.getWindow().getContext().getSelected() instanceof TextInputControl )
					return;
				
				Game.copySelection();
			}
			
			// Paste object
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_V ) {
				if ( object.getWindow().getContext().getSelected() instanceof TextInputControl )
					return;
				
				Instance pasteInto = Game.workspace();
				List<Instance> t = Game.selected();
				if ( t.size() == 1 ) {
					pasteInto = t.get(0);
					if ( event.isShiftDown && !pasteInto.getParent().isnil() ) {
						pasteInto = (Instance) pasteInto.getParent();
					}
				}
				
				Game.paste(pasteInto);
			}
			
			
			
			
			// Undo control
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_Z ) {
				if ( object.getWindow().getContext().getSelected() instanceof TextInputControl )
					return;
				
				Game.historyService().undo();
			}
			
			// Redo control
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_Y ) {
				if ( object.getWindow().getContext().getSelected() instanceof TextInputControl )
					return;
				
				Game.historyService().redo();
			}
			
			
			
			
			// Save control
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_S ) {
				Save.save();
			}
			
			// New control
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_N ) {
				Game.newProject();
			}
		});
	}

}
