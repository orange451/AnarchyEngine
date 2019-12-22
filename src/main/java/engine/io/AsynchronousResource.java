/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.io;

public abstract class AsynchronousResource<E> {
	protected String filePath;
	
	public AsynchronousResource(String path) {
		this.filePath = path;
	}
	
	public String getPath() {
		return filePath;
	}
	
	public abstract E getResource();
	public abstract boolean isLoaded();
	protected abstract void internalLoad();
}
