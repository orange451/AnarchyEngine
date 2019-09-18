package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.NumberClamp;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class DynamicSkybox extends Instance implements TreeViewable {

	private static final LuaValue C_BRIGHTNESS = LuaValue.valueOf("Brightness");
	private static final LuaValue C_TIME = LuaValue.valueOf("Time");
	private static final LuaValue C_CLOUDHEIGHT = LuaValue.valueOf("CloudHeight");
	private static final LuaValue C_CLOUDSPEED = LuaValue.valueOf("CloudSpeed");

	public DynamicSkybox() {
		super("DynamicSkybox");
		
		this.defineField(C_BRIGHTNESS.toString(), LuaValue.valueOf(1), false);
		this.getField(C_BRIGHTNESS).setClamp(new NumberClampPreferred(0, 10, 0, 5));
		
		this.defineField(C_TIME.toString(), LuaValue.valueOf(0), false);
		this.getField(C_TIME).setClamp(new NumberClamp(0, 24000));
		
		this.defineField(C_CLOUDHEIGHT.toString(), LuaValue.valueOf(3500), false);
		this.getField(C_CLOUDHEIGHT).setClamp(new NumberClampPreferred(1000, 60000, 0, 10000));
		
		this.defineField(C_CLOUDSPEED.toString(), LuaValue.valueOf(1), false);
		this.getField(C_CLOUDSPEED).setClamp(new NumberClampPreferred(-100, 100, -10, 10));
	}
	
	public float getTime() {
		return this.get(C_TIME).tofloat();
	}
	
	public void setTime(float time) {
		this.set(C_TIME, LuaValue.valueOf(time));
	}
	
	public float getCloudHeight() {
		return this.get(C_CLOUDHEIGHT).tofloat();
	}
	
	public void setCloudHeight(float height) {
		this.set(C_CLOUDHEIGHT, LuaValue.valueOf(height));
	}
	
	public float getCloudSpeed() {
		return this.get(C_CLOUDSPEED).tofloat();
	}
	
	public void setCloudSpeed(float height) {
		this.set(C_CLOUDSPEED, LuaValue.valueOf(height));
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
		return Icons.icon_sky;
	}
	
	public void setBrightness( float brightness ) {
		this.set(C_BRIGHTNESS, LuaValue.valueOf(brightness));
	}

	public float getBrightness() {
		return this.rawget(C_BRIGHTNESS).tofloat();
	}
}
