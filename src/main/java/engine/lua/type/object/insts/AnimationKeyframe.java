/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts;

import org.joml.Matrix4f;
import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Matrix4;
import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class AnimationKeyframe extends Instance implements TreeViewable {

	public AnimationKeyframe() {
		super("AnimationKeyframe");
		this.setInstanceable(false);
		
		this.getField(LuaValue.valueOf("Archivable")).setLocked(true);
		this.defineField("Matrix", new Matrix4(), false);
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

	public Matrix4 getMatrix() {
		LuaValue ret = this.get("Matrix");
		return ret==null?null:(Matrix4)ret;
	}
	
	public Matrix4f getMatrixInternal() {
		Matrix4 mat = getMatrix();
		if ( mat == null )
			return null;
		
		return mat.getInternal();
	}
}
