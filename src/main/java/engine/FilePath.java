/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine;

import java.io.File;

import engine.util.FileUtils;

public class FilePath {
	
	public static final String PROJECT_IDENTIFIER = "%PROJECT%";

	/**
	 * Convert a URL string to a full system filepath.
	 * @param ideFilePath
	 * @return
	 */
	public static String convertToSystem(String ideFilePath) {
		return FileUtils.fixPath(ideFilePath.replace(PROJECT_IDENTIFIER, new File(Game.saveDirectory).getAbsolutePath()));
	}

	/**
	 * Convert a full system filepath to a project-relative filepath.
	 * @param systemFilePath
	 * @return
	 */
	public static String convertToIDE(String systemFilePath) {
		return FileUtils.fixPath(systemFilePath.replace(new File(Game.saveDirectory).getAbsolutePath(), PROJECT_IDENTIFIER));
	}
}
