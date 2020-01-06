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

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class Animation extends Instance implements TreeViewable {

	public Animation() {
		super("Animation");
		
		this.setInstanceable(false);
		
		this.getField(LuaValue.valueOf("Archivable")).setLocked(true);

		this.defineField("Speed", LuaValue.valueOf(1.0), false);
		this.defineField("Looped", LuaValue.valueOf(false), false);
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

	public float getSpeed() {
		return this.get("Speed").tofloat();
	}

	public boolean isLooped() {
		return this.get("Looped").toboolean();
	}

	/**
	 * Calculates the max time of the animation.
	 * @return
	 */
	public double getMaxTime() {
		double len = 0;
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			if ( child instanceof AnimationKeyframeSequence ) {
				AnimationKeyframeSequence seq = (AnimationKeyframeSequence)child;
				if ( seq.getTime() > len )
					len = seq.getTime();
			}
		}
		
		return len;
	}
	
	/**
	 * Returns the AnimationKeyframeSequence with the time closest to the specified time, but not greater than the specified time.
	 * @param time
	 * @return
	 */
	public AnimationKeyframeSequence getNearestSequenceBefore(double time) {
		double minTime = -1;
		AnimationKeyframeSequence ret = null;
		
		if ( children == null || children.size() == 0 )
			return null;
		
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			if ( child instanceof AnimationKeyframeSequence ) {
				AnimationKeyframeSequence seq = (AnimationKeyframeSequence)child;
				double seqTime = seq.getTime();
				if ( seqTime <= time ) {
					if ( seqTime >= minTime ) {
						minTime = seqTime;
						ret = seq;
					}
				}
			}
		}
	
		return ret;
	}
		
	/**
	 * Returns the AnimationKeyframeSequence with the time closest to the specified time, but not less than the specified time.
	 * @param time
	 * @return
	 */
	public AnimationKeyframeSequence getNearestSequenceAfter(double time) {
		double maxTime = Double.MAX_VALUE;
		AnimationKeyframeSequence ret = null;
		
		if ( children == null || children.size() == 0 )
			return null;
		
		for (int i = 0; i < children.size(); i++) {
			Instance child = children.get(i);
			if ( child instanceof AnimationKeyframeSequence ) {
				AnimationKeyframeSequence seq = (AnimationKeyframeSequence)child;
				double seqTime = seq.getTime();
				if ( seqTime >= time ) {
					if ( seqTime <= maxTime ) {
						maxTime = seqTime;
						ret = seq;
					}
				}
			}
		}
	
		return ret;
	}

	public AnimationKeyframeSequence getFirstSequence() {
		return this.getNearestSequenceAfter(0);
	}
}
