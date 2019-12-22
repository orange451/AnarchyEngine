/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.objects;

import static org.lwjgl.opengl.GL32C.*;

public class CubeMapTexture extends Texture {

	public CubeMapTexture(int texture, int size) {
		super(texture, GL_TEXTURE_CUBE_MAP, size, size);
	}

	@Deprecated
	public int getSize() {
		return super.getWidth();
	}

}
