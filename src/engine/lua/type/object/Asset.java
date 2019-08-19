package engine.lua.type.object;

import org.luaj.vm2.LuaValue;

import engine.lua.LuaEngine;

public abstract class Asset extends Instance {

	public Asset(String type) {
		super(type);
		
		this.setInstanceable(true);
	}
	
	@Override
	public void set(LuaValue key, LuaValue value) {
		if ( key.eq_b(C_PARENT) && getPreferredParent() != null ) {
			if ( !value.isnil() && !value.equals(getPreferredParent()) && !this.isDescendantOf(value) ) {
				LuaEngine.error("Asset type: " + this.typename() + " must exist within: " + this.getPreferredParent().getFullName());
				return;
			}
		}
		super.set(key, value);
	}

	public abstract Instance getPreferredParent();
}
