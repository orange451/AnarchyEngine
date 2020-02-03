/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.objects;

import static org.lwjgl.system.MemoryUtil.memFree;

import java.nio.ByteBuffer;

public class RawLTC extends RawTexture {

	public RawLTC(ByteBuffer buffer, int width, int height, int comp) {
		super(buffer, width, height, comp);
	}
	
	@Override
	public void dispose() {
		memFree(buffer);
	}

}
