package engine.util;

import static org.lwjgl.opengl.GL11.GL_R;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import engine.InternalRenderThread;
import engine.gl.Texture2D;
import engine.io.Image;
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
		
		InternalRenderThread.runLater(() -> {
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
