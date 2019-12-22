/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.mesh.mm3d;

import engine.gl.Resources;
import engine.gl.Texture2D;
import engine.util.TextureUtils;

public class Mm3dTexture {
	private int flags;
	private String path;
	
	private Texture2D diffuseTexture;
	
	public Mm3dTexture(String filePath, long getuInt16, String string) {
		this.flags = (int) getuInt16;
		this.path = string;
		
		String path = filePath + string;
		path = path.replace("\\", "/");
		path = path.replace("/./", "/");
		
		if ( path.contains(".") )
			this.diffuseTexture = TextureUtils.loadSRGBTexture(path);
		else
			this.diffuseTexture = Resources.TEXTURE_WHITE_SRGB;
	}
	
	public String getFullPath() {
		return this.path;
	}

	public Texture2D getDiffuseTexture() {
		return diffuseTexture;
	}
}
