/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object;

import org.luaj.vm2.LuaValue;

import engine.Game;
import engine.lua.LuaEngine;

public abstract class Asset extends Instance {

	public Asset(String type) {
		super(type);
		
		this.setInstanceable(true);
	}
	
	@Override
	public void set(LuaValue key, LuaValue value) {
		LuaValue preferred = this.getPreferredParent();
		if ( key.eq_b(C_PARENT) && preferred != null ) {
			if ( !value.isnil() ) {
				Instance newParent = (Instance)value;
				Instance preferredParent = Game.assets().findFirstChild(getPreferredParent());
				if ( preferredParent != null && !newParent.equals(preferredParent) && !newParent.isDescendantOf(preferredParent) ) {
					LuaEngine.error("Asset type: " + this.typename() + " must exist within: " + preferredParent.getFullName());
					return;
				}
			}
		}
		super.set(key, value);
	}

	public abstract LuaValue getPreferredParent();
}
