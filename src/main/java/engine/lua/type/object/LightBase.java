package engine.lua.type.object;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.gl.light.Light;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.util.AABBUtil;
import engine.util.Pair;

public abstract class LightBase extends Instance implements Positionable {

	protected static final LuaValue C_POSITION = LuaValue.valueOf("Position");
	protected static final LuaValue C_INTENSITY = LuaValue.valueOf("Intensity");
	protected static final LuaValue C_COLOR = LuaValue.valueOf("Color");
	protected static final LuaValue C_SHADOWS = LuaValue.valueOf("Shadows");
	protected static final LuaValue C_VISIBLE = LuaValue.valueOf("Visible");
	
	public LightBase(String typename) {
		super(typename);

		this.defineField(C_POSITION.toString(), new Vector3(), false);
		
		this.defineField(C_INTENSITY.toString(), LuaValue.valueOf(1), false);
		this.getField(C_INTENSITY).setClamp(new NumberClampPreferred(0, 100, 0, 8));
		
		this.defineField(C_COLOR.toString(), Color3.white(), false);
		this.defineField(C_SHADOWS.toString(), LuaValue.valueOf(true), false);
		this.defineField(C_VISIBLE.toString(), LuaValue.valueOf(true), false);
		
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
		this.set(C_VISIBLE, LuaValue.valueOf(visible));
	}
	
	public boolean isVisible() {
		return this.get(C_VISIBLE).toboolean();
	}

	public void setIntensity(float intensity) {
		this.set(C_INTENSITY, LuaValue.valueOf(intensity));
	}
	
	public float getIntensity() {
		return this.get(C_INTENSITY).tofloat();
	}

	public void setPosition(int x, int y, int z) {
		this.set(C_POSITION, new Vector3(x, y, z));
	}
	
	public void setPosition(Vector3 position) {
		this.set(C_POSITION, position.clone());
	}
	
	@Override
	public Vector3 getPosition() {
		return (Vector3) this.get(C_POSITION);
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
