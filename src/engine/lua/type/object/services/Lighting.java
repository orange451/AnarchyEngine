package engine.lua.type.object.services;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Color3;
import engine.lua.type.object.Service;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Lighting extends Service implements TreeViewable {

	public Lighting() {
		super("Lighting");
		
		this.defineField("Ambient", Color3.newInstance(128, 128, 128), false);
		this.defineField("Exposure", LuaValue.valueOf(1.0f), false);
		this.defineField("Saturation", LuaValue.valueOf(1.2f), false);
		this.defineField("Gamma", LuaValue.valueOf(2.2f), false);
	}
	
	public Color3 getAmbient() {
		return (Color3) this.get("Ambient");
	}
	
	public void setAmbient(Color3 color) {
		this.set("Ambient", color);
	}
	
	public float getExposure() {
		return this.get("Exposure").tofloat();
	}
	
	public void setExposure(float value) {
		this.set("Exposure", value);
	}
	
	public float getSaturation() {
		return this.get("Saturation").tofloat();
	}
	
	public void setSaturation(float value) {
		this.set("Saturation", value);
	}
	
	public float getGamma() {
		return this.get("Gamma").tofloat();
	}
	
	public void setGamma(float value) {
		this.set("Gamma", value);
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
	public Icons getIcon() {
		return Icons.icon_light;
	}
}
