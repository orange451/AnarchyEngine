package engine.lua.type.object;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.gl.light.Light;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.util.AABBUtil;
import engine.util.Pair;

public abstract class LightBase extends Instance implements Positionable {
	public LightBase(String typename) {
		super(typename);

		this.defineField("Position", Vector3.newInstance(0, 0, 0), false);
		this.defineField("Intensity", LuaValue.valueOf(1), false);
		this.defineField("Color", Color3.white(), false);
		this.defineField("Shadows", LuaValue.valueOf(true), false);
		this.defineField("Visible", LuaValue.valueOf(true), false);
		
		this.changedEvent().connect((args)->{
			if ( args[0].toString().equals("Visible") ) {
				Light l = this.getLightInternal();
				if ( l == null )
					return;
				
				l.visible = args[1].toboolean();
			}
		});
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}
	
	public abstract Light getLightInternal();
	
	public void setVisible( boolean visible ) {
		this.set("Visible", LuaValue.valueOf(visible));
	}
	
	public boolean isVisible() {
		return this.get("Visible").toboolean();
	}

	public void setIntensity(float intensity) {
		this.set("Intensity", LuaValue.valueOf(intensity));
	}

	public void setPosition(int x, int y, int z) {
		this.set("Position", Vector3.newInstance(x, y, z));
	}
	
	public void setPosition(Vector3 position) {
		this.set("Position", position.clone());
	}
	
	@Override
	public Vector3 getPosition() {
		return (Vector3) this.get("Position");
	}
	
	@Override
	public Pair<Vector3f, Vector3f> getAABB() {
		return AABBUtil.newAABB(new Vector3f(), new Vector3f());
	}
	
	@Override
	public Matrix4 getWorldMatrix() {
		return new Matrix4(getPosition());
	}
}
