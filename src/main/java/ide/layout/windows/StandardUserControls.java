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
			
			// Delete
			if ( event.getKey() == GLFW.GLFW_KEY_DELETE ) {
				List<Instance> selected = Game.selected();
				for (int i = 0; i < selected.size(); i++) {
					selected.get(i).destroy();
				}
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
