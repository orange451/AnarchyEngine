package engine.gl.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.net.URL;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import engine.gl.Pipeline;
import engine.gl.Surface;
import engine.gl.shader.BaseShader;

public class TransparencyRenderer {
	private TBufferShader shader;
	private Pipeline pipeline;
	
	private int fboId;
	private int textureId;
	
	private int width;
	private int height;
	
	public TransparencyRenderer(Pipeline pipeline, int x, int y) {
		this.pipeline = pipeline;
		this.shader = new TBufferShader();
		resize(x, y);
	}
	
	/**
	 * Bind the Transparency renderer. Shares depth buffer with GBuffer. Disables writing to depth buffer.
	 */
	public void bind() {
		glViewport(0, 0, width, height);
		
		glEnable(GL_DEPTH_TEST);
		glDepthMask(false);

		glBindTexture(GL_TEXTURE_2D, 0);
		glBindFramebuffer(GL_FRAMEBUFFER, fboId);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glClearColor(0, 0, 0, 0);
		glClear(GL_COLOR_BUFFER_BIT);
		
		pipeline.shader_set(shader);

		shader.shader_set_uniform_f(shader.shader_get_uniform("uAmbient"), pipeline.getGBuffer().getAmbient());
		
	}

	/**
	 * Unbinds transparency renderer. Re-enables writing to depth buffer.
	 */
	public void unbind() {
		glDepthMask(true);
		glDisable(GL_BLEND);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}
	
	/**
	 * Resize the Transparency renderer's FBO.
	 * @param width
	 * @param height
	 */
	public void resize(int width, int height) {
		// Cleanup
		if ( textureId > 0 )
			GL11.glDeleteTextures(textureId);
		if ( fboId > 0 )
			GL30.glDeleteFramebuffers(fboId);

		this.width = width;
		this.height = height;
		
		// Create new fbo
		this.fboId = glGenFramebuffers();
		this.textureId = glGenTextures();
		
		// Create color texture
		glBindTexture(GL_TEXTURE_2D, textureId);
		{
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (java.nio.ByteBuffer)null);
		}
		glBindTexture(GL_TEXTURE_2D, 0);
		
		// Bind textures to FBO
		glBindFramebuffer(GL_FRAMEBUFFER, fboId);
		{
			glBindTexture (GL_TEXTURE_2D, textureId); // Attach our color texture
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);
			glBindTexture (GL_TEXTURE_2D, pipeline.getGBuffer().getBufferDepth().getID()); // Attach gbuffers depth texture
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, pipeline.getGBuffer().getBufferDepth().getID(), 0);
		}
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		
	}

	static class TBufferShader extends BaseShader {
		public TBufferShader() {
			super(
				new URL[] {
					GBuffer.class.getResource("forward.vert")
				},
				new URL[] {
					GBuffer.class.getResource("normalmap.frag"),
					GBuffer.class.getResource("reflect.frag"),
					GBuffer.class.getResource("fresnel.frag"),
					GBuffer.class.getResource("reflectivePBR.frag"),
					GBuffer.class.getResource("forward.frag")
				}
			);
		}
	}

	public int getBuffer() {
		return textureId;
	}
}
