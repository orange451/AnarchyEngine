package engine.gl.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.net.URL;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import engine.gl.Pipeline;
import engine.gl.SkyBox;
import engine.gl.ibl.SkySphereIBL;
import engine.gl.light.PointLightInternal;
import engine.gl.shader.BaseShader;

public class TransparencyRenderer {
	private static TBufferShader shader;
	private Pipeline pipeline;
	
	private int fboId;
	private int textureId;
	
	private int width;
	private int height;
	
	public TransparencyRenderer(Pipeline pipeline, int x, int y) {
		this.pipeline = pipeline;
		
		if ( shader == null )
			shader = new TBufferShader();
		
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
		SkyBox skybox = pipeline.getGBuffer().getMergeProcessor().getSkybox();
		if ( skybox != null ) {
			shader.shader_set_uniform_f(shader.shader_get_uniform("uSkyBoxLightPower"), ((SkySphereIBL)skybox).getLightPower());
			shader.shader_set_uniform_f(shader.shader_get_uniform("uSkyBoxLightMultiplier"), ((SkySphereIBL)skybox).getLightMultiplier());
		}
		
		// Send point light data
		// TODO replace this with a Uniform buffer. MUCH BETTER
		PointLightInternal[] pointLights = pipeline.getGBuffer().getLightProcessor().getPointLightHandler().getLights();
		Matrix4f viewMatrix = pipeline.getGBuffer().getViewMatrix();
		shader.shader_set_uniform_f(shader.shader_get_uniform("uNumPointLights"), pointLights.length);
		for (int i = 0; i < pointLights.length; i++) {
			PointLightInternal light = pointLights[i];
			
			Vector3f lightEyePos = new Vector3f(light.position.x, light.position.y, light.position.z);
			viewMatrix.transformPosition(lightEyePos, lightEyePos);
			
			shader.shader_set_uniform_f(shader.shader_get_uniform("uPointLights["+i+"].Position"), lightEyePos);
			shader.shader_set_uniform_f(shader.shader_get_uniform("uPointLights["+i+"].Color"), light.color);
			shader.shader_set_uniform_f(shader.shader_get_uniform("uPointLights["+i+"].Radius"), light.radius);
			shader.shader_set_uniform_f(shader.shader_get_uniform("uPointLights["+i+"].Intensity"), light.intensity);
		}
		
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
					PointLightInternal.class.getResource("pbr.frag"),
					PointLightInternal.class.getResource("pointlight.frag"),
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
