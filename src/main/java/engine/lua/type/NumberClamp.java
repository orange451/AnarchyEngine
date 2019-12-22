/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type;

import org.luaj.vm2.LuaValue;

public class NumberClamp extends Clamp<Float> {
	private float min;
	private float max;
	
	public NumberClamp(float min, float max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public LuaValue clamp(LuaValue value) {
		return LuaValue.valueOf(Math.min(getMax(), Math.max(getMin(), value.tofloat())));
	}

	@Override
	public Float getMin() {
		return min;
	}

	@Override
	public Float getMax() {
		return max;
	}
}
