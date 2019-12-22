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

import org.joml.Vector3f;

import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.util.Pair;

public interface Positionable {
	public Vector3 getPosition();
	public void setPosition(Vector3 position);
	public Matrix4 getWorldMatrix();
	public Pair<Vector3f, Vector3f> getAABB();
}
