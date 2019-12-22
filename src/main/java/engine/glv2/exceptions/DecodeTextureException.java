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

public class DecodeTextureException extends RuntimeException {

	private static final long serialVersionUID = -8629683316677759946L;

	public DecodeTextureException(String e) {
		super(e);
	}

	public DecodeTextureException(Exception e) {
		super(e);
	}

}
