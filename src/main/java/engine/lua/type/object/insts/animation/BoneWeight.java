/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.animation;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class BoneWeight extends Instance implements TreeViewable {

	private static final LuaValue C_VERTEXID = LuaValue.valueOf("VertexId");
	private static final LuaValue C_WEIGHT = LuaValue.valueOf("Weight");
	
	public BoneWeight() {
		super("BoneWeight");

		this.defineField(C_VERTEXID.toString(), LuaValue.valueOf(-1), true);
		this.defineField(C_WEIGHT.toString(), LuaValue.valueOf(0.0), true);
		
		this.setInstanceable(false);

		this.getField(LuaValue.valueOf("Archivable")).setLocked(true);
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	public int getVertexId() {
		return this.get(C_VERTEXID).toint();
	}
	
	public float getWeight() {
		return this.get(C_WEIGHT).tofloat();
	}
	
	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_film_timeline;
	}
}
