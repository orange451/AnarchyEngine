package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class RayResult extends Instance implements TreeViewable {

	protected static final LuaValue C_OBJECT = LuaValue.valueOf("HitObject");
	protected static final LuaValue C_POSITION = LuaValue.valueOf("HitPosition");
	protected static final LuaValue C_NORMAL = LuaValue.valueOf("HitNormal");
	
	public RayResult() {
		super("RayResult");

		this.defineField(C_OBJECT.toString(), LuaValue.NIL, true);
		this.defineField(C_POSITION.toString(), new Vector3(), true);
		this.defineField(C_NORMAL.toString(), new Vector3(), true);
		
		this.setInstanceable(false);
	}
	
	public RayResult(PhysicsBase object, Vector3 position, Vector3 normal) {
		this();
		
		if ( object != null)
			this.rawset(C_OBJECT,  object);
		
		if ( position != null )
			((Vector3)this.rawget(C_POSITION)).setInternal(position.getInternal());
		
		if ( normal != null )
			((Vector3)this.rawget(C_NORMAL)).setInternal(normal.getInternal());
	}
	
	public PhysicsBase getHitObject() {
		LuaValue t = this.get(C_OBJECT);
		return t.isnil()?null:(PhysicsBase)t;
	}
	
	public Vector3 getHitPosition() {
		return (Vector3) this.get(C_POSITION);
	}
	
	public Vector3 getHitNormal() {
		return (Vector3) this.get(C_NORMAL);
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
		return Icons.icon_value;
	}
}
