package ide.layout;

import lwjgui.geometry.Orientation;
import lwjgui.scene.control.SplitPane;

public class IdeVerticalDock extends SplitPane {
	private IdeDockPane north;
	private IdeDockPane south;
	
	public IdeVerticalDock() {
		this.setOrientation(Orientation.HORIZONTAL);
		this.setFillToParentHeight(true);
		this.setFillToParentWidth(true);
		update();
	}
	
	public void update() {
		this.getItems().clear();
		
		// Add north/south
		if ( north != null && north.getTabs().size() > 0)
			this.getItems().add(north);
		if ( south != null && south.getTabs().size() > 0 )
			this.getItems().add(south);
	}

	public void dockNorth(IdePane pane) {
		if ( north == null )
			north = new IdeDockPane();
		
		north.dock(pane);
		update();
	}

	public void dockSouth(IdePane pane) {
		if ( south == null )
			south = new IdeDockPane();
		
		south.dock(pane);
		update();
	}
}
