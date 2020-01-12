package engine;

import engine.lua.type.object.services.UserInputService;
import lwjgui.event.ScrollEvent;
import lwjgui.scene.Node;
import lwjgui.scene.layout.StackPane;

public class AnarchyEngineClientUIFrame extends StackPane {
	
	public AnarchyEngineClientUIFrame() {
		
		this.setOnKeyPressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			if ( !this.isDescendentSelected() )
				return;
			
			uis.onKeyPressed(event.getKey());
		});
		this.setOnKeyReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			if ( !this.isDescendentSelected() )
				return;
			
			uis.onKeyReleased(event.getKey());
		});
		this.setOnMousePressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			uis.onMousePress(event.button);
			cached_context.setSelected(this);
		});
		this.setOnMouseReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			uis.onMouseRelease(event.button);
		});
		this.setOnMouseScrolled(event ->{
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			if ( uis == null )
				return;
			
			if ( !this.isDescendentHovered() && !this.cached_context.isHovered(this) )
				return;
			
			uis.onMouseScroll(((ScrollEvent)event).y > 0 ? 3 : 4 );
		});
	}
	
	@Override
	public void position(Node parent) {
		super.position(parent);
		this.forceSize(parent.getWidth(), parent.getHeight());
	}
}
