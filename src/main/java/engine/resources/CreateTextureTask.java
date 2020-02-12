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

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;

import engine.gl.objects.RawTexture;
import engine.gl.objects.Texture;
import engine.tasks.Task;
import lwjgui.paint.Color;

public class CreateTextureTask extends Task<Texture> {

	private Color color;
	private int width, height;
	private int filter;
	private int textureWarp;
	private int format;
	private boolean textureMipMapAF;

	public CreateTextureTask(Color color, int width, int height, int filter, int textureWarp, int format,
			boolean textureMipMapAF) {
		this.color = color;
		this.width = width;
		this.height = height;
		this.filter = filter;
		this.textureWarp = textureWarp;
		this.format = format;
		this.textureMipMapAF = textureMipMapAF;
	}

	@Override
	protected Texture call() {
		RawTexture rawTexture = RawTexture.fromColor(color, width, height);
		int id = ResourcesManager.backend.loadTexture(filter, textureWarp, format, GL_UNSIGNED_BYTE, textureMipMapAF,
				rawTexture);
		rawTexture.dispose();
		System.out.println("Loaded: " + color);
		return new Texture(id, GL_TEXTURE_2D, rawTexture.getWidth(), rawTexture.getHeight());
	}

}
