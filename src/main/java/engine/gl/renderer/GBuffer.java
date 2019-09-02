package engine.gl.renderer;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL30.glBindFragDataLocation;

import java.net.URL;
import java.util.ArrayList;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import engine.Game;
import engine.gl.Pipeline;
import engine.gl.PostProcessor;
import engine.gl.Surface;
import engine.gl.Texture2D;
import engine.gl.shader.BaseShader;
import lwjgui.paint.Color;

public class GBuffer {
	private int buffer0Tex; // Albedo
	private int buffer1Tex; // Normals
	private int buffer2Tex; // Metal/Rough
	private int buffer3Tex; // Emissive
	
	private Surface buffer;
	private GBufferShader shader;
	private Pipeline pipeline;
	
	private Surface finalBuffer;
	private Surface accumulationBuffer;
	
	private MergeProcessor merge;
	private LightProcessor lightProcessor;
	private ArrayList<PostProcessor> processors = new ArrayList<PostProcessor>();
	
	private Vector3f ambient = new Vector3f( 0.03f );
	private float exposure = 1.0f;
	private float gamma = 2.2f;
	private float saturation = 1.2f;

	private Matrix4f viewMatrix = new Matrix4f();
	private Matrix4f iViewMatrix = new Matrix4f();
	private Matrix4f projMatrix = new Matrix4f();
	private Matrix4f iProjMatrix = new Matrix4f();
	
	public GBuffer(Pipeline pipeline, int width, int height) {
		this.shader = new GBufferShader();
		this.pipeline = pipeline;
		
		resize(width, height);
		
		processors.add(lightProcessor = new LightProcessor());
		processors.add(merge = new MergeProcessor());
		processors.add(new ToneMapper());
	}
	
	public int getBuffer0() {
		return buffer0Tex;
	}
	
	public int getBuffer1() {
		return buffer1Tex;
	}
	
	public int getBuffer2() {
		return buffer2Tex;
	}
	
	public int getBuffer3() {
		return buffer3Tex;
	}
	
	public Texture2D getBufferDepth() {
		return buffer.getTextureDepth();
	}
	
	public Surface getBufferFinal() {
		return this.finalBuffer;
	}
	
	public Surface getAccumulationBuffer() {
		return this.accumulationBuffer;
	}
	
	public void cleanup() {
		if ( buffer != null )
			buffer.cleanup();
		
		buffer = null;
	}
	
	public void bind() {
		buffer.bind();
		buffer.draw_clear_alpha(Color.BLACK, 1.0f);
		
		pipeline.shader_set(shader);

		viewMatrix.set(pipeline.getViewMatrix());
		viewMatrix.invert(iViewMatrix);
		projMatrix.set(pipeline.getProjectionMatrix());
		projMatrix.invert(iProjMatrix);
		
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
	}

	public void unbind() {
		buffer.unbind();
	}

	public void resize(int width, int height) {
		if ( buffer != null ) {
			buffer.cleanup();
		}
		if ( finalBuffer != null ) {
			finalBuffer.cleanup();
		}
		
		if ( accumulationBuffer != null ) {
			accumulationBuffer.cleanup();
		}
		
		// Generate surface
		Surface.filter = GL11.GL_LINEAR;
		{
			// R11 G11 B10  (better quality colors (but) low data size, no alpha)
			{
				finalBuffer = new Surface(width, height, GL30.GL_RGBA16F);
				buffer = new Surface(width, height, GL30.GL_R11F_G11F_B10F);
			}
			
			accumulationBuffer = new Surface(width, height, GL30.GL_RGBA16F);
			buffer0Tex = buffer.getTextureId();
			buffer1Tex = buffer.generateTexture( false, GL30.GL_RGBA32F ); // Normals
			buffer2Tex = buffer.generateTexture( false, GL11.GL_RGBA8 );   // PBR materials
			buffer3Tex = buffer.generateTexture( false, GL30.GL_RGBA16F ); // Emissive
		}
		Surface.filter = GL11.GL_LINEAR;

		// create two more channels
		buffer.bindTexture(buffer1Tex, GL30.GL_COLOR_ATTACHMENT1);
		buffer.bindTexture(buffer2Tex, GL30.GL_COLOR_ATTACHMENT2);
		buffer.bindTexture(buffer3Tex, GL30.GL_COLOR_ATTACHMENT3);
	}
	
	static class GBufferShader extends BaseShader {
		public GBufferShader() {
			super(
				new URL[] {
						GBuffer.class.getResource("deferred.vert")
				},
				new URL[] {
						GBuffer.class.getResource("normalmap.frag"),
						GBuffer.class.getResource("reflect.frag"),
						GBuffer.class.getResource("fresnel.frag"),
						GBuffer.class.getResource("reflectivePBR.frag"),
						GBuffer.class.getResource("write.frag"),
						GBuffer.class.getResource("deferred.frag")
				}
			);
		}
		
		@Override
		public void create(int id) {
			glBindFragDataLocation(id, 0, "gBuffer0");
			glBindFragDataLocation(id, 1, "gBuffer1");
			glBindFragDataLocation(id, 2, "gBuffer2");
			glBindFragDataLocation(id, 3, "gBuffer3");
		}
	}

	public BaseShader getShader() {
		return this.shader;
	}
	
	public void postProcess() {
		GL11.glDisable(GL_CULL_FACE);
		GL11.glDisable(GL_DEPTH_TEST);
		pipeline.ortho();
		
		if ( Game.isLoaded() ) {
			this.saturation = Game.lighting().getSaturation();
			this.exposure = Game.lighting().getExposure();
			this.gamma = Game.lighting().getGamma();
			Color color = Game.lighting().getAmbient().toColor();
			this.ambient = new Vector3f( 
					Math.max(1/255f, color.getRed()/255f),
					Math.max(1/255f, color.getGreen()/255f),
					Math.max(1/255f, color.getBlue()/255f) );
		}
		
		for (int i = 0; i < processors.size(); i++) {
			processors.get(i).process(pipeline);
		}
	}

	/**
	 * Returns the current ambient color for the scene.
	 * @return
	 */
	public Vector3f getAmbient() {
		return ambient;
	}

	/**
	 * Returns the current-used view matrix for rendering the GBuffer.
	 * @return
	 */
	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	/**
	 * Returns the current-used view matrix for rendering the GBuffer.
	 * @return
	 */
	public Matrix4f getInverseViewMatrix() {
		return iViewMatrix;
	}

	/**
	 * Returns the current-used projection matrix for rendering the GBuffer.
	 * @return
	 */
	public Matrix4f getProjectionMatrix() {
		return projMatrix;
	}

	/**
	 * Returns the current-used inverse projection matrix for rendering the GBuffer.
	 * @return
	 */
	public Matrix4f getInverseProjectionMatrix() {
		return iProjMatrix;
	}

	public MergeProcessor getMergeProcessor() {
		return merge;
	}
	
	/**
	 * Returns this gbuffers light processor. Can be used to access the scenes lights.
	 * @return
	 */
	public LightProcessor getLightProcessor() {
		return lightProcessor;
	}

	/**
	 * Return the current camera-exposure value used for tone-mapping.
	 * @return
	 */
	public float getExposure() {
		return this.exposure;
	}

	/**
	 * Return the current camera-gamma value used for tone-mapping.
	 * @return
	 */
	public float getGamma() {
		return this.gamma;
	}

	/**
	 * Return the current camera-saturation value used for tone-mapping;
	 * @return
	 */
	public float getSaturation() {
		return this.saturation;
	}
}
