/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public final class IOUtil {
	
	public static URL ioResourceGetURL( String resource ) {
		// Search inside jar
		URL url = IOUtil.class.getClassLoader().getResource(resource);
		
		// File not found. Search outside jar
		if (url == null) {
			try {
				url = new File(resource).toURI().toURL();
			} catch (MalformedURLException e) {
				// NO FILE
				return null;
			}
		}

		// Return url;
		return url;
	}

}
