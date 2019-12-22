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

public abstract class Clamp<T> {
	public abstract LuaValue clamp(LuaValue value);
	public abstract T getMin();
	public abstract T getMax();
}
