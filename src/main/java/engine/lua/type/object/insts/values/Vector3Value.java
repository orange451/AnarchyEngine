/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.values;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.Instance;
import engine.lua.type.object.Positionable;
import engine.lua.type.object.TreeViewable;
import engine.util.AABBUtil;
import engine.util.Pair;
import ide.layout.windows.icons.Icons;

public class Vector3Value extends ValueBase implements TreeViewable,Positionable {

	protected static final LuaValue C_VALUE = LuaValue.valueOf("Value");
	
	public Vector3Value() {
		super("Vector3Value");
		
		this.defineField(C_VALUE.toString(), new Vector3(), false);
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
	
	public Vector3 getValue() {
		LuaValue value = this.get(C_VALUE);
		return value.isnil()?null:(Vector3)value;
	}
	
	public void setValue(Vector3 value) {
		this.set(C_VALUE, value.clone());
	}

	@Override
	public Vector3 getPosition() {
		return this.getValue();
	}

	@Override
	public void setPosition(Vector3 position) {
		this.setValue(position);
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
