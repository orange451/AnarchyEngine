/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.io;

import engine.util.TextureUtils;
import lwjgui.paint.Color;

public class AsynchronousImage extends AsynchronousResource<Image> {
	private static Image TEMP;
	
	static {
		TEMP = new Image(Color.WHITE,1,1);
	}
	
	private Image image;
	private boolean flipY;
	
	public AsynchronousImage(String path, boolean flipY) {
		super(path);
		this.flipY = flipY;
	}

	@Override
	public Image getResource() {
		if ( !isLoaded() ) {
			return TEMP;
		}
		
		return image;
	}

	@Override
	public boolean isLoaded() {
		return image != null;
	}

	@Override
	protected void internalLoad() {
		Image i = TextureUtils.loadImage(this.filePath, flipY);
		if ( i.loaded() ) {
			image = i;
		}
	}
}
