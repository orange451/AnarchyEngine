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
		Instance preferred = this.getPreferredParent();
		if ( key.eq_b(C_PARENT) && preferred != null ) {
			if ( !value.isnil() ) {
				Instance newParent = (Instance)value;
				if ( !value.equals(preferred) && !newParent.isDescendantOf(preferred) ) {
					LuaEngine.error("Asset type: " + this.typename() + " must exist within: " + this.getPreferredParent().getFullName());
					return;
				}
			}
		}
		super.set(key, value);
	}

	public abstract Instance getPreferredParent();
}
