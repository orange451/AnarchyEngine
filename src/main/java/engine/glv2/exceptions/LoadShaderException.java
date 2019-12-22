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

public class LoadShaderException extends RuntimeException {

	private static final long serialVersionUID = -5645376169749026843L;

	public LoadShaderException() {
		super();
	}

	public LoadShaderException(String error) {
		super(error);
	}

	public LoadShaderException(Exception e) {
		super(e);
	}

	public LoadShaderException(Throwable cause) {
		super(cause);
	}

	public LoadShaderException(String message, Throwable cause) {
		super(message, cause);
	}

}
