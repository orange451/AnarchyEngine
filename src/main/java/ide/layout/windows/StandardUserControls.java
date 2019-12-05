package ide.layout.windows;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import engine.Game;
import engine.io.Save;
import engine.lua.type.object.Instance;
import lwjgui.LWJGUI;
import lwjgui.scene.Node;

public class StandardUserControls {

	public static void bind(Node object) {
		object.setOnKeyPressed((event)->{
			// Node must be selected
			if ( !LWJGUI.getCurrentContext().isSelected(object) && !object.isDescendentSelected() )
				return;
			
			// Delete Selection
			if ( event.getKey() == GLFW.GLFW_KEY_DELETE ) {
				Game.deleteSelection();
			}
			
			// Duplicate object
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_D ) {
				Game.duplicateSelection();
			}
			
			// Cut object
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_X ) {
				Game.cutSelection();
			}
			
			// Copy object
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_C ) {
				Game.copySelection();
			}
			
			// Paste object
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_C ) {
				Instance sel = Game.workspace();
				List<Instance> t = Game.selected();
				if ( t.size() == 1 )
					sel = t.get(0);
				
				Game.paste(sel);
			}
			
			
			
			
			// Undo control
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_Z ) {
				Game.historyService().undo();
			}
			
			// Redo control
			if ( event.isCtrlDown && event.getKey() == GLFW.GLFW_KEY_Y ) {
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
