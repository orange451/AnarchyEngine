/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.exceptions;

public class LoadTextureException extends RuntimeException {

	private static final long serialVersionUID = 960750507253812538L;

	public LoadTextureException(Exception cause) {
		super(cause);
	}

	public LoadTextureException(String texture, Exception cause) {
		super(cause);
	}

}
