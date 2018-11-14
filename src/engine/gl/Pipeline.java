package engine.gl;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.luaj.vm2.LuaValue;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import engine.Game;
import engine.application.RenderableApplication;
import engine.gl.gbuffer.GBuffer;
import engine.gl.mesh.BufferedMesh;
import engine.gl.shader.BaseShader;
import engine.observer.Renderable;
import engine.observer.RenderableInstance;
import engine.util.MeshUtils;
import luaengine.type.object.Instance;
import luaengine.type.object.insts.Camera;

public class Pipeline implements Renderable {
	private boolean enabled = true;
	private Vector2i size = new Vector2i();
	private Surface buffer;
	private GBuffer gbuffer;
	private BaseShader genericShader;
	
	private Matrix4f viewMatrix;
	private Matrix4f projMatrix;
	private BufferedMesh fullscreenMesh;
	
	private BaseShader currentShader;
	
	private List<Renderable> renderables;
	
	private static Pipeline currentPipeline;
	
	private static final Matrix4f IDENTITY = new Matrix4f().identity();

	public Pipeline() {
		this.setSize(RenderableApplication.windowWidth, RenderableApplication.windowHeight);
		genericShader = new BaseShader();
		buffer = new Surface(size.x,size.y, GL11.GL_RGBA8);
		gbuffer = new GBuffer(this, size.x,size.y);
		viewMatrix = new Matrix4f();
		projMatrix = new Matrix4f();
		renderables = Collections.synchronizedList(new ArrayList<Renderable>());
	}
	
	public void setSize(int width, int height) {
		if ( width < 1 || height < 1 ) {
			width = 1;
			height = 1;
		}
		if ( size.x == width && size.y == height )
			return;
		
		this.size.x = width;
		this.size.y = height;

		if ( buffer != null ) {
			this.buffer.cleanup();
			this.buffer = new Surface(width,height);
		}
		if ( gbuffer != null )
			this.gbuffer.resize(width, height);

		if ( fullscreenMesh == null )
			fullscreenMesh = MeshUtils.quad(1, 1);
	}

	public void attachRenderable(Renderable renderable) {
		synchronized(renderables) {
			renderables.add(renderable);
		}
	}
	
	public void detachRenderable(Renderable renderable) {
		synchronized(renderables) {
			renderables.remove(renderable);
		}
	}

	@Override
	public void render() {
		if ( !enabled )
			return;
		
		// Update current pipeline
		currentPipeline = this;
		
		// Update camera matrices
		perspective();
		
		// Draw to GBuffer
		gbuffer.bind();
		{
			setOpenGLState();
			
			// Render workspace into gbuffer
			renderInstance(gbuffer.getShader(), Game.workspace());
			
			// Render everything attached
			synchronized(renderables) {
				for (int i = 0; i < renderables.size(); i++) {
					Renderable r = renderables.get(i);
					r.render();
				}
			}
			
			// Render selected instances
			List<Instance> instances = Game.selected();
			synchronized(instances) {
				for (int i = 0; i < instances.size(); i++) {
					Instance instance = instances.get(i);
					OutlineRenderer.render(instance);
				}
			}
		}
		gbuffer.unbind();
		
		// Do post processing
		gbuffer.postProcess();
		
		// Draw GBuffer to back-buffer
		buffer.bind();
		{
			GL11.glDisable(GL_CULL_FACE);
			GL11.glDisable(GL_DEPTH_TEST);
			
			// Set shader
			ortho();
			shader_set(genericShader);
			
			// Draw main
			drawTexture( gbuffer.getBufferFinal(), 0, 0, size.x, size.y );
			
			// Draw buffers
			boolean debug = false;
			if ( debug ) {
				drawTexture( gbuffer.getBuffer0(), 0,   0, 100, 100 );
				drawTexture( gbuffer.getBuffer1(), 100, 0, 100, 100 );
				drawTexture( gbuffer.getBuffer2(), 200, 0, 100, 100 );
				drawTexture( gbuffer.getBuffer3(), 300, 0, 100, 100 );
				drawTexture( gbuffer.getAccumulationBuffer(), 400, 0, 100, 100 );
				drawTexture( gbuffer.getMergeProcessor().getBuffer(), 500, 0, 100, 100 );
			}
		}
		buffer.unbind();
	}
	
	public GBuffer getGBuffer() {
		return this.gbuffer;
	}
	
	public void fullscreenQuad( ) {
		drawQuad( 0, 0, size.x, size.y);
	}
	
	private void drawQuad( int x, int y, int width, int height ) {
		fullscreenMesh.render(shader_get(), new Matrix4f().translate(x, y, 0).scale(width, height,1), null);
	}
	
	private void drawTexture( int buffer, int x, int y, int width, int height) {
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		glBindTexture(GL11.GL_TEXTURE_2D, buffer);
		drawQuad( x, y, width, height );
	}
	
	private void drawTexture( Surface surface, int x, int y, int width, int height) {
		drawTexture(surface.getTextureId(),x,y,width,height);
	}

	private void setOpenGLState() {
		// Face culling and depth testing...
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
	}

	private void renderInstance(BaseShader shader, Instance root) {
		List<Instance> instances = root.getChildren();
		for (int i = 0; i < instances.size(); i++) {
			Instance inst = instances.get(i);
			renderInstance(shader, inst);
		}
		
		if ( root instanceof RenderableInstance ) {
			((RenderableInstance)root).render(shader);
		}
	}
	
	public void ortho() {
		projMatrix = new Matrix4f().ortho(0, size.x, 0, size.y, -3200, 3200);
		viewMatrix = new Matrix4f(IDENTITY);
	}
	
	private void perspective() {
		LuaValue cam = Game.workspace().get("CurrentCamera");
		if ( !cam.equals(LuaValue.NIL) ) {
			Camera camera = (Camera)cam;
			float fov = camera.get("Fov").tofloat();
			float aspect = (float)size.x/(float)size.y;
			
			// Set matrices
			viewMatrix = camera.getViewMatrix().toJoml();
			projMatrix.identity().perspective((float) Math.toRadians(fov), aspect, 0.1f, 512);
		} else {
			viewMatrix = gbuffer.getViewMatrix();
			projMatrix = gbuffer.getProjectionMatrix();
		}
	}

	public BaseShader shader_set(BaseShader shader) {
		shader.bind();
		shader.setProjectionMatrix(projMatrix);
		shader.setViewMatrix(viewMatrix);
		shader.setWorldMatrix(IDENTITY);
		currentShader = shader;
		return shader;
	}
	
	/**
	 * Returns the last pipeline to start render calls
	 * @return
	 */
	public static Pipeline pipeline_get() {
		return currentPipeline;
	}
	
	public BaseShader shader_reset() {
		return shader_set(genericShader);
	}
	
	public BaseShader shader_get() {
		return this.currentShader;
	}

	/**
	 * The final 3d rendered scene with post-processing
	 * @return
	 */
	public Surface getPipelineBuffer() {
		return buffer;
	}
	
	/**
	 * Sets the enabled state. If not enabled, no drawing will be done.
	 * @param enabled
	 */
	public void setEnabled( boolean enabled ) {
		this.enabled = enabled;
	}

	/**
	 * Returns the current-used view matrix for rendering the scene.
	 * @return
	 */
	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	/**
	 * Returns the current-used projection matrix for rendering the scene.
	 * @return
	 */
	public Matrix4f getProjectionMatrix() {
		return projMatrix;
	}

	/**
	 * Returns the current renderable canvas width of the pipeline
	 * @return
	 */
	public int getWidth() {
		return size.x;
	}

	/**
	 * Returns the current renderable canvas height of the pipeline
	 * @return
	 */
	public int getHeight() {
		return size.y;
	}
}
