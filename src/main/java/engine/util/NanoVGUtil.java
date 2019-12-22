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

import org.lwjgl.nanovg.NVGColor;

public class NanoVGUtil {
	public static NVGColor color( int r, int g, int b, int a ) {
		NVGColor ret = NVGColor.create();
		ret.r(r/255.0f);
		ret.g(g/255.0f);
		ret.b(b/255.0f);
		ret.a(a/255.0f);
		
		return ret;
	}
}
