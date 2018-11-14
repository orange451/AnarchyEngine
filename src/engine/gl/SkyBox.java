package engine.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import engine.io.Image;

public class SkyBox {
	protected int cubemap;
	protected int framebuffer;
	protected int depthbuffer;
	protected ByteBuffer imageBuffer;

	protected int width;
	protected int height;

	public SkyBox(Image image){
		cubemap = glGenTextures();

		int oldWidth = width;
		int oldHeight = height;
		width = image.getWidth() / 4;
		height = image.getHeight() / 3;
		if(width != height){
			throw new IllegalArgumentException("Invalid sky box dimensions.");
		}

		int size = width;

		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP);
		glEnable(GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS);

		int components = image.getComponents();

		if ( imageBuffer == null || (oldWidth != width || oldHeight != height) )
			imageBuffer = BufferUtils.createByteBuffer(size * size * components);

		int internalFormat = GL_RGBA16F;
		int externalFormat = GL_RGBA;
		int type = GL_UNSIGNED_BYTE;

		//TextureUtils.loadImageBuffer(image, size*2, size, size, size, handler, imageBuffer);
		glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, size, size, 0, externalFormat, type, imageBuffer);
		GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, size*2, size, size, size, externalFormat, type, image.getData());

		//TextureUtils.loadImageBuffer(image, 0, size, size, size, handler, imageBuffer);
		glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, size, size, 0, externalFormat, type, imageBuffer);
		GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, 0, size, size, size, externalFormat, type, image.getData());

		//TextureUtils.loadImageBuffer(image, size, 0, size, size, handler, imageBuffer);
		glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, size, size, 0, externalFormat, type, imageBuffer);
		GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, size, 0, size, size, externalFormat, type, image.getData());

		//TextureUtils.loadImageBuffer(image, size, size*2, size, size, handler, imageBuffer);
		glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, size, size, 0, externalFormat, type, imageBuffer);
		GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, size, size*2, size, size, externalFormat, type, image.getData());

		//TextureUtils.loadImageBuffer(image, size, size, size, size, handler, imageBuffer);
		glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, size, size, 0, externalFormat, type, imageBuffer);
		GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, size, size, size, size, externalFormat, type, image.getData());

		//TextureUtils.loadImageBuffer(image, size*3, size, size, size, handler, imageBuffer);
		glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, size, size, 0, externalFormat, type, imageBuffer);
		GL11.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, size*3, size, size, size, externalFormat, type, image.getData());

		glGenerateMipmap(GL_TEXTURE_CUBE_MAP);

        // create the fbo
		framebuffer = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        // create the uniform depth buffer
		depthbuffer = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, depthbuffer);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
		glBindRenderbuffer(GL_RENDERBUFFER, 0);

        // attach it
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, framebuffer);

        // attach only the +X cubemap texture (for completeness)
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X, cubemap, 0);

        // disable
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
	}

	public void bind(){
		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap);
		glEnable(GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS);
	}

	public static void unbind(){
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
	}

	public boolean draw(Pipeline pipeline) {
		return true;
	}

	public void destroy() {
		//
	}

	public int getTextureID() {
		return cubemap;
	}
}
