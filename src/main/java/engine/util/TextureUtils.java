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

import static org.lwjgl.opengl.GL11.GL_R;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import engine.gl.Texture2D;
import engine.io.Image;
import engine.tasks.TaskManager;
import lwjgui.paint.Color;

public class TextureUtils {
	public static boolean autoResize = false;
	
	public static int MAX_TEXTURE_SIZE = 1024;
	
	public static Texture2D loadRGBATexture(String filename) {
		return loadTexture(filename, GL11.GL_LINEAR, GL11.GL_LINEAR_MIPMAP_LINEAR, false);
	}

	public static Texture2D loadSRGBTexture(String filename) {
		return loadTexture(filename, GL11.GL_LINEAR, GL11.GL_LINEAR_MIPMAP_LINEAR, true);
	}

	public static Texture2D loadSRGBTextureFromImage(Image image) {
		return loadTexture(image, GL11.GL_LINEAR, GL11.GL_LINEAR_MIPMAP_LINEAR, true);
	}

	public static Texture2D loadRGBATextureFromImage(Image image) {
		return loadTexture(image, GL11.GL_LINEAR, GL11.GL_LINEAR_MIPMAP_LINEAR, false);
	}
	
	public static Texture2D loadTexture(String filename, int internalFormat, int externalFormat) {
		return loadTexture( loadImage(filename), GL11.GL_LINEAR, GL11.GL_LINEAR_MIPMAP_LINEAR, internalFormat, externalFormat);
	}
	
	public static Image loadImage( String path ) {
		return loadImage( path, false );
	}
	
	public static Image loadImage( String path, boolean flipY ) {
		if ( path == null || path.length() == 0 || path.contains("NULL") )
			return new Image(Color.WHITE,1,1);
		
		Image image = new Image( path, flipY );

		// Resize if too big
		int maxRes = MAX_TEXTURE_SIZE;
		if ( autoResize && image.getData() != null && (image.getWidth() > maxRes || image.getHeight() > maxRes) ) {
			image.resize( maxRes, maxRes );
		}

		return image;
	}
	
	private static Texture2D loadTexture(Image image, int near, int far, int internalFormat, int externalFormat) {
		// Create new texture
		Texture2D texture = new Texture2D(GL_TEXTURE_2D, internalFormat, GL_UNSIGNED_BYTE, externalFormat);

		TaskManager.addTaskRenderThread(() -> {
			// Generate texture
			texture.gen();
			
			// Bind
			texture.bind();
			
			// Load image to texture
			texture.load(0, image.getWidth(), image.getHeight(), image.getData());
			
			// Set filters
			if (near > -1 && far > -1)
				texture.setMipMapFilter(near, far);
			
			// Generate mipmaps
			texture.generateMipmaps();
		});
		
		return texture;
	}
	
	private static Texture2D loadTexture(Image image, int near, int far, boolean srgb) {
		// Get format
		int internalFormat = srgb?GL21.GL_SRGB8_ALPHA8:GL_RGBA;
		int externalFormat = GL_RGBA;
		if (image.getComponents() == 3) {
			internalFormat = srgb?GL21.GL_SRGB8:GL_RGB;
			externalFormat = GL_RGB;
		}
		if (image.getComponents() == 1) {
			internalFormat = GL_R;
			externalFormat = GL_R;
		}
		
		return loadTexture(image, near, far, internalFormat, externalFormat);
	}
	
	private static Texture2D loadTexture(String filename, int near, int far, boolean srgb) {
		Image image = loadImage(filename);
		if ( image == null )
			return null;
		
		return loadTexture(image, near, far, srgb);
	}
}
