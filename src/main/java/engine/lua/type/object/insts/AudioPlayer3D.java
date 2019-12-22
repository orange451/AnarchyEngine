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

import engine.Game;
import engine.lua.lib.EnumType;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Matrix4;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.AudioPlayerBase;
import engine.lua.type.object.Positionable;
import engine.util.AABBUtil;
import engine.util.Pair;

public class AudioPlayer3D extends AudioPlayerBase implements Positionable {
	protected static final LuaValue C_POSITION = LuaValue.valueOf("Position");
	protected static final LuaValue C_POSITIONTYPE = LuaValue.valueOf("PositionType");
	protected static final LuaValue C_RANGE = LuaValue.valueOf("Range");
	
	public AudioPlayer3D() {
		super("AudioPlayer3D");

		this.defineField(C_POSITION.toString(), new Vector3(), false);

		this.defineField(C_POSITIONTYPE.toString(), LuaValue.valueOf("Relative"), false);
		this.getField(C_POSITIONTYPE).setEnum(new EnumType("PositionType"));

		this.defineField(C_RANGE.toString(), LuaValue.valueOf(8.0f), false);
		this.getField(C_RANGE).setClamp(new NumberClampPreferred(0, 32, 0, 1024 ));
	}
	
	public void setRange(float range) {
		this.set(C_RANGE, LuaValue.valueOf(range));
	}
	
	public float getRange() {
		return this.get(C_RANGE).tofloat();
	}
	
	@Override
	public Vector3 getPosition() {
		return (Vector3) this.get(C_POSITION);
	}

	public void setPosition(Vector3 position) {
		this.set(C_POSITION, position.clone());
	}

	@Override
	public Pair<Vector3f, Vector3f> getAABB() {
		return AABBUtil.newAABB(new Vector3f(), new Vector3f());
	}
	
	@Override
	public Matrix4 getWorldMatrix() {
		return new Matrix4(getPosition());
	}
	
	@Override
	public void playSource() {
		AudioSource source = getSource();
		if ( source == null )
			return;
		
		Game.soundService().playSound3D(source, getPosition(), getVolume(), getPitch(), getRange());
	}
}
