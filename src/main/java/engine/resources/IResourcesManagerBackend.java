/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.resources;

import engine.gl.objects.RawTexture;

public interface IResourcesManagerBackend {

	public int loadTexture(int filter, int textureWarp, int format, boolean textureMipMapAF, RawTexture data);

}
