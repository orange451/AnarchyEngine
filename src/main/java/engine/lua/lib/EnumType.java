/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.lib;

public class EnumType extends LuaTableReadOnly {
	private String type;
	
	public EnumType(String string) {
		this.type = string;
	}

	public String getType() {
		return this.type;
	}
}
