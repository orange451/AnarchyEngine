package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.NumberClampPreferred;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Skybox extends Instance implements TreeViewable {

	public Skybox() {
		super("Skybox");
		
		this.defineField("Image", LuaValue.NIL, false);
		
		this.defineField("Brightness", LuaValue.valueOf(2), false);
		this.getField("Brightness").setClamp(new NumberClampPreferred(0, 10, 0, 5));
		
		this.defineField("Power", LuaValue.valueOf(2), false);
		this.getField("Power").setClamp(new NumberClampPreferred(0, 10, 0, 5));
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_skybox;
	}

	public Texture getImage() {
		LuaValue ret = this.rawget("Image");
		return ret.isnil()?null:(Texture)ret;
	}

	public float getPower() {
		return this.rawget("Power").tofloat();
	}

	public float getBrightness() {
		return this.rawget("Brightness").tofloat();
	}
}
