package engine.lua.type.object.insts.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.lib.EnumType;
import engine.lua.lib.Enums;
import engine.lua.type.LuaConnection;
import engine.lua.type.LuaEvent;
import engine.lua.type.data.Vector2;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import lwjgui.geometry.Pos;
import lwjgui.scene.Node;
import lwjgui.style.Stylesheet;

public abstract class GuiBase extends Instance implements TreeViewable {

	protected static final LuaValue C_CLASSLIST = LuaValue.valueOf("ClassList");
	protected static final LuaValue C_SIZE = LuaValue.valueOf("Size");
	protected static final LuaValue C_ALIGNMENT = LuaValue.valueOf("Alignment");
	protected static final LuaValue C_MOUSETRANSPARENT = LuaValue.valueOf("MouseTransparent");
	protected static final LuaValue C_CLICKEDEVENT = LuaValue.valueOf("MouseClicked");
	protected static final LuaValue C_MOUSEENTERED = LuaValue.valueOf("MouseEntered");
	protected static final LuaValue C_MOUSEEXITED = LuaValue.valueOf("MouseExited");
	protected static final LuaValue C_MOUSEPRESSED = LuaValue.valueOf("MousePressed");
	protected static final LuaValue C_MOUSERELEASED = LuaValue.valueOf("MouseReleased");
	protected static final LuaValue C_CSS = LuaValue.valueOf("CSS");
	
	
	protected Map<Instance, LuaConnection> uiConnections = new HashMap<>();
	protected Gui gui;

	public GuiBase(String name) {
		super(name);
		
		this.defineField(C_CLASSLIST.toString(), new engine.lua.type.data.List(), false);

		this.defineField(C_SIZE.toString(), new Vector2(0, 0), false);
		
		this.defineField(C_ALIGNMENT.toString(), LuaValue.valueOf("TopLeft"), false);
		this.getField(C_ALIGNMENT).setEnum(new EnumType("GuiAlignment"));
		
		this.defineField(C_MOUSETRANSPARENT.toString(), LuaValue.FALSE, false);

		this.rawset(C_CLICKEDEVENT, new LuaEvent());
		this.rawset(C_MOUSEENTERED, new LuaEvent());
		this.rawset(C_MOUSEEXITED, new LuaEvent());
		this.rawset(C_MOUSEPRESSED, new LuaEvent());
		this.rawset(C_MOUSERELEASED, new LuaEvent());
		
		this.changedEvent().connect((args)->{
			if ( args[0].eq_b(C_PARENT) ) {
				rebuildCSS();
			}
		});
		
		this.descendantAddedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof CSS ) {
				
				// Track changes
				rebuildCSS();
				LuaConnection connection = ((CSS)arg).changedEvent().connect((cargs)->{
					rebuildCSS();
				});
				
				// Add connection to map
				uiConnections.put((GuiBase) arg, connection);
			}
		});
		
		this.descendantRemovedEvent().connect((args)->{
			LuaValue arg = args[0];
			if ( arg instanceof CSS ) {
				rebuildCSS();
				
				// Disconnect connection
				LuaConnection connection = uiConnections.get((Instance)arg);
				if ( connection != null )
					connection.disconnect();
				
				// Add connection to map
				uiConnections.remove(arg);
			}
		});
	}
	
	/**
	 * Mouse Clicked Event. Fires when the mouse is released when this node is hovered,
	 * but only when the mouse was previously pressed while this node was hovered too.
	 * The mouse button (integer) is passed as the first VARARG.
	 * @return
	 */
	public LuaEvent getMouseClickedEvent() {
		LuaValue t = this.get(C_CLICKEDEVENT);
		return t.isnil()?null:(LuaEvent)t;
	}
	
	/**
	 * Mouse Entered event. Fires when the mouse enters the rectangle bounds of this gui object.
	 * @return
	 */
	public LuaEvent getMouseEnteredEvent() {
		LuaValue t = this.get(C_MOUSEENTERED);
		return t.isnil()?null:(LuaEvent)t;
	}
	
	/**
	 * Mouse Exited Event. Fires when the mouse leaves the rectangle bounds of this gui object.
	 * @return
	 */
	public LuaEvent getMouseExitedEvent() {
		LuaValue t = this.get(C_MOUSEEXITED);
		return t.isnil()?null:(LuaEvent)t;
	}
	
	/**
	 * Mouse Pressed Event. Fires when the mouse presses down when this gui object is hovered.
	 * The mouse button (integer) is passed as the first VARARG.
	 * @return
	 */
	public LuaEvent getMousePressedEvent() {
		LuaValue t = this.get(C_MOUSEPRESSED);
		return t.isnil()?null:(LuaEvent)t;
	}
	
	/**
	 * Mouse Released Event. Fires when the mouse is released when this gui object is hovered.
	 * The mouse button (integer) is passed as the first VARARG.
	 * @return
	 */
	public LuaEvent getMouseReleasedEvent() {
		LuaValue t = this.get(C_MOUSERELEASED);
		return t.isnil()?null:(LuaEvent)t;
	}
	
	private void rebuildCSS() {
		String style = "";
		
		List<Instance> css = this.getChildrenOfClass(C_CSS);
		for (int i = 0; i < css.size(); i++) {
			CSS cssObj = (CSS) css.get(i);
			style += cssObj.getSource();
		}
		
		Stylesheet sheet = new Stylesheet(style);
		Node node = this.gui.getNode(this);
		if ( node == null )
			return;
		
		node.setStylesheet(sheet);
	}

	/**
	 * Set the 2 dimensional preferred size for the gui element.
	 * @param vector
	 */
	public void setSize(Vector2 vector) {
		this.set(C_SIZE, new Vector2(vector.toJoml()));
	}
	
	/**
	 * Get the 2 dimensional preferred size for the gui element.
	 * @return
	 */
	public Vector2 getSize() {
		LuaValue val = this.get(C_SIZE);
		return val.isnil()?new Vector2():(Vector2)val;
	}

	/**
	 * Return the preferred width of this gui element.
	 * @return
	 */
	public float getWidth() {
		return this.getSize().getX();
	}

	/**
	 * Return the preferred height of this gui element.
	 * @return
	 */
	public float getHeight() {
		return this.getSize().getY();
	}

	/**
	 * Get the inner-alignment for this gui element. This will control how direction children are positioned inside this element.
	 * @return
	 */
	public String getAlignment() {
		LuaValue alignment = Enums.matchEnum(LuaValue.valueOf("GuiAlignment"), this.get(C_ALIGNMENT));
		return alignment.isnil()?Pos.CENTER.toString():alignment.toString();
	}
	
	/**
	 * Returns whether the inner node will ignore mouse events.
	 * @return
	 */
	public boolean isMouseTransparent() {
		return this.get(C_MOUSETRANSPARENT).checkboolean();
	}
	
	/**
	 * Sets the mouse transparency variable. See {@link #isMouseTransparent()}.
	 * @param transparent
	 */
	public void setMouseTransparent(boolean transparent) {
		this.set(C_MOUSETRANSPARENT, LuaValue.valueOf(transparent));
	}
	
	public abstract Node getUINode();
	
	protected void updateNodeInternal(Node node) {
		
		// Set click event
		if ( node.getOnMouseClicked() == null ) {
			node.setOnMouseClicked((event)->{
				if ( Game.isRunning() )
					getMouseClickedEvent().fire(LuaValue.valueOf(event.button));
			});
			node.setOnMouseEntered((event)->{
				if ( Game.isRunning() )
					getMouseEnteredEvent().fire();
			});
			node.setOnMouseExited((event)->{
				if ( Game.isRunning() )
					getMouseExitedEvent().fire();
			});
			node.setOnMousePressed((event)->{
				if ( Game.isRunning() )
					getMousePressedEvent().fire(LuaValue.valueOf(event.button));
			});
			node.setOnMouseReleased((event)->{
				if ( Game.isRunning() )
					getMouseReleasedEvent().fire(LuaValue.valueOf(event.button));
			});
		}
		
		// Update element Id
		node.setElementId(getName());
		
		// Mouse transparency
		node.setMouseTransparent(this.isMouseTransparent());
		
		// Apply class list
		engine.lua.type.data.List list = (engine.lua.type.data.List) this.get(C_CLASSLIST);
		node.getClassList().clear();
		for (int i = 0; i < list.size(); i++)
			node.getClassList().add(list.getElement(i).toString());
	}
	
	public abstract void updateNode(Node node);
}
