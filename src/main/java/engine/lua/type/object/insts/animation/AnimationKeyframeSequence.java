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

import java.util.ArrayList;
import java.util.List;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class AnimationKeyframeSequence extends Instance implements TreeViewable {

	protected List<AnimationKeyframe> keyframes;
	
	private static LuaValue C_TIME = LuaValue.valueOf("Time");
	
	public AnimationKeyframeSequence() {
		super("AnimationKeyframeSequence");
		
		this.setInstanceable(false);
		
		this.getField(LuaValue.valueOf("Archivable")).setLocked(true);
		
		this.defineField(C_TIME.toString(), LuaValue.valueOf(0), true);
		
		keyframes = new ArrayList<AnimationKeyframe>();
		this.childAddedEvent().connect((args)->{
			if ( args.length != 1 )
				return;
			
			LuaValue child = args[0];
			if ( !(child instanceof AnimationKeyframe) )
				return;
			
			AnimationKeyframe keyframe = (AnimationKeyframe) child;
			keyframes.add( keyframe);
		});
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

	public double getTime() {
		return this.get(C_TIME).todouble();
	}
}
