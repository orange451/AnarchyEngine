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

public class NumberClampPreferred extends NumberClamp {
	private float preferredMin;
	private float preferredMax;
	
	public NumberClampPreferred(float min, float max, float preferredMin, float preferredMax) {
		super(min,max);
		
		this.preferredMin = preferredMin;
		this.preferredMax = preferredMax;
	}
	
	public float getPreferredMin() {
		return this.preferredMin;
	}
	
	public float getPreferredMax() {
		return this.preferredMax;
	}
}
