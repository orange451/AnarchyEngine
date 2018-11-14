package ide.layout;

import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.scene.layout.StackPane;

public abstract class IdePane extends StackPane {
	public String paneName;
	public boolean closable;
	public IdeDockPane dockedTo;
	
	public IdePane(String name, boolean closable) {
		this.paneName = name;
		this.closable = closable;
		
		setPadding(Insets.EMPTY);
		setAlignment(Pos.TOP_LEFT);
		
		this.setFillToParentHeight(true);
		this.setFillToParentWidth(true);
	}
	
	public String getName() {
		return paneName;
	}
	
	public abstract void onOpen();
	public abstract void onClose();
}
