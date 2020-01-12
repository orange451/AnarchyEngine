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

import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import engine.lua.type.object.insts.Mesh;
import ide.layout.windows.icons.Icons;

public class Bone extends Instance implements TreeViewable {

	protected static final LuaValue C_MESH = LuaValue.valueOf("Mesh");
	protected static final LuaValue C_OFFSETMATRIX = LuaValue.valueOf("OffsetMatrix");
	
	public Bone() {
		super("Bone");
		
		this.defineField(C_MESH.toString(), LuaValue.NIL, true);
		this.defineField("OffsetMatrix", new Matrix4(), true);
		
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

	@Override
	public void onDestroy() {
		//
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_film;
	}
	
	public Mesh getMesh() {
		LuaValue mesh = this.get(C_MESH);
		return mesh.isnil()?null:(Mesh)mesh;
	}

	public Matrix4 getOffsetMatrix() {
		return (Matrix4) this.get(C_OFFSETMATRIX);
	}

	public void setOffsetMatrix(Matrix4 matrix4) {
		this.forceset(C_OFFSETMATRIX, matrix4);
	}
}
