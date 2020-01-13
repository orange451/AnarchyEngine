/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.script;

import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class GlobalScript extends ScriptBase implements TreeViewable {

	public GlobalScript() {
		super("GlobalScript");
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_script_global;
	}

	@Override
	public boolean getCanRun() {
		return true;
	}
}
