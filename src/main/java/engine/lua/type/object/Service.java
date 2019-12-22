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

public abstract class Service extends Instance {

	public Service(String name) {
		super(name);
		this.setInstanceable(false);
		
		this.getField(C_NAME).setLocked(true);
		this.getField(C_PARENT).setLocked(true);
		this.getField(C_ARCHIVABLE).setLocked(true);
		
		this.setLocked(true);
	}

	public void onDestroy() {
		//
	}
}
