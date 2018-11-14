package luaengine.type.object;

import org.luaj.vm2.LuaValue;

import luaengine.LuaEngine;

public abstract class Asset extends Instance {

	public Asset(String type) {
		super(type);
		
		this.setInstanceable(true);
	}
	
	@Override
	public void set(LuaValue key, LuaValue value) {
		if ( key.toString().equals("Parent") && getPreferredParent() != null ) {
			if ( !value.isnil() && !value.equals(getPreferredParent()) ) {
				LuaEngine.error("Asset type: " + this.typename() + " must exist within: " + this.getPreferredParent().getFullName());
				return;
			}
		}
		super.set(key, value);
	}

	public abstract Instance getPreferredParent();
}
