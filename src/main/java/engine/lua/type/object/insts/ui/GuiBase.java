package engine.lua.type.object.insts.ui;

import org.luaj.vm2.LuaValue;

import engine.lua.lib.EnumType;
import engine.lua.lib.Enums;
import engine.lua.type.data.Vector2;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import lwjgui.geometry.Pos;
import lwjgui.scene.Node;

public abstract class GuiBase extends Instance implements TreeViewable {

	protected static final LuaValue C_SIZE = LuaValue.valueOf("Size");
	protected static final LuaValue C_ALIGNMENT = LuaValue.valueOf("Alignment");

	public GuiBase(String name) {
		super(name);

		this.defineField(C_SIZE.toString(), new Vector2(0, 0), false);
		
		this.defineField(C_ALIGNMENT.toString(), LuaValue.valueOf("TopLeft"), false);
		this.getField(C_ALIGNMENT).setEnum(new EnumType("GuiAlignment"));
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
	
	public abstract Node getUINode();
	
	public abstract void updateNode(Node node);
}
