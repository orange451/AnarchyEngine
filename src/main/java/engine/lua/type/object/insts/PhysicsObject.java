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

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.lua.type.object.PhysicsBase;
import engine.lua.type.object.TreeViewable;
import engine.util.Pair;
import ide.layout.windows.icons.Icons;

public class PhysicsObject extends PhysicsBase implements TreeViewable {

	public PhysicsObject() {
		super("PhysicsObject");
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_wat;
	}

	@Override
	public Pair<Vector3f, Vector3f> getAABB() {
		Pair<Vector3f, Vector3f>  aabb = new Pair<Vector3f, Vector3f>(getPosition().toJoml(), getPosition().toJoml());
		
		if ( linked == null )
			return aabb;
		
		LuaValue p = linked.get(C_PREFAB);
		if ( !p.isnil() && p instanceof Prefab ) {
			Prefab p2 = (Prefab) p;
			aabb = p2.getPrefab().getAABB();
		}
		
		return aabb;
	}
}
