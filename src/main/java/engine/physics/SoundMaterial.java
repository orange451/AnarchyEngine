/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.physics;

public class SoundMaterial {
	private final String name;
	private final String directory;
	
	public SoundMaterial(String name, String directory) {
		this.name = name;
		this.directory = directory;
	}
	
	public String getDirectory() {
		return directory + name + "/";
	}
}
