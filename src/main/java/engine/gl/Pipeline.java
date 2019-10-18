package engine.gl;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glBindTexture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import engine.Game;
import engine.application.RenderableApplication;
import engine.gl.lua.HandlesRenderer;
import engine.gl.lua.OutlineRenderer;
import engine.gl.mesh.BufferedMesh;
import engine.gl.renderer.GBuffer;
import engine.gl.renderer.TransparencyRenderer;
import engine.gl.shader.BaseShader;
import engine.glv2.v2.lights.IDirectionalLightHandler;
import engine.glv2.v2.lights.IPointLightHandler;
import engine.lua.type.object.Instance;
import engine.lua.type.object.PrefabRenderer;
import engine.lua.type.object.insts.AnimationController;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.GameObject;
import engine.lua.type.object.insts.Model;
import engine.lua.type.object.insts.Prefab;
import engine.observer.Renderable;
import engine.observer.RenderableInstance;
import engine.observer.RenderableMesh;
import engine.observer.RenderableWorld;
import engine.util.MeshUtils;
import engine.util.Pair;

public class Pipeline implements IPipeline {
	private boolean enabled = true;
	private Vector2i size = new Vector2i(1,1);
	private Surface buffer;
	private GBuffer gbuffer;
	private TransparencyRenderer tbuffer;
	private BaseShader genericShader;
	
	private Matrix4f viewMatrix;
	private Matrix4f projMatrix;
	private Camera currentCamera;
	private BufferedMesh fullscreenMesh;
	
	private RenderableWorld renderableWorld; 
	private BaseShader currentShader;
	private List<Renderable> renderables;
	
	private static IPipeline currentPipeline;
	private static HashMap<RenderableWorld, IPipeline> pipelineMap = new HashMap<>();
	
	private boolean debug;
	
	private static final Matrix4f IDENTITY = new Matrix4f().identity();

	public Pipeline() {
		this.setSize(RenderableApplication.windowWidth, RenderableApplication.windowHeight);
		genericShader = new BaseShader();
		gbuffer = new GBuffer(this, size.x,size.y);
		tbuffer = new TransparencyRenderer(this, size.x,size.y);
		viewMatrix = new Matrix4f();
		projMatrix = new Matrix4f();
		renderables = Collections.synchronizedList(new ArrayList<Renderable>());
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public void setRenderableWorld(RenderableWorld instance) {
		this.renderableWorld = instance;
		pipelineMap.put(instance, this);
	}
	
	public RenderableWorld getRenderableWorld() {
		return this.renderableWorld;
	}
	
	public static void set(IPipeline pipeline, RenderableWorld instance) {
		pipelineMap.put(instance, pipeline);
	}
	
	public static IPipeline get(RenderableWorld world) {
		return pipelineMap.get(world);
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

		if ( buffer != null )
			this.buffer.cleanup();
		this.buffer = new Surface(width,height, GL11.GL_RGBA8);
			
		if ( gbuffer != null )
			this.gbuffer.resize(width, height);
		
		if ( tbuffer != null )
			this.tbuffer.resize(width, height);

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

	//private ArrayList<RenderableInstance> transparentObjects = new ArrayList<RenderableInstance>();
	//private ArrayList<RenderableMesh> transparentMeshes = new ArrayList<RenderableMesh>();
	
	private ArrayList<Pair<Float,Pair<RenderableMesh,Pair<Matrix4f,MaterialGL>>>> transparencies = new ArrayList<Pair<Float,Pair<RenderableMesh,Pair<Matrix4f,MaterialGL>>>>();

	@Override
	public void render() {
		if ( !enabled )
			return;
		
		if ( this.renderableWorld == null )
			return;
		
		// Update current pipeline
		currentPipeline = this;
		
		// Update camera matrices
		perspective();
		
		// Draw to GBuffer
		gbuffer.bind();
		{
			
			// Render workspace into gbuffer (also seperates out special renderables)
			synchronized(transparencies) {
				transparencies.clear();
				renderInstancesRecursive(gbuffer.getShader(), renderableWorld.getInstance());
			}
			
			// Render everything attached
			synchronized(renderables) {
				for (int i = 0; i < renderables.size(); i++) {
					Renderable r = renderables.get(i);
					r.render();
				}
			}
			
			// Render selected instances
			List<Instance> instances = Game.selectedExtended();
			synchronized(instances) {
				for (int i = 0; i < instances.size(); i++) {
					Instance instance = instances.get(i);
					OutlineRenderer.render(instance);
				}
			}
		}
		gbuffer.unbind();
		
		tbuffer.bind();
		{
			// Order and render all transparent geometry
			synchronized(transparencies) {
				
				// Sort based on distance
				Vector3f tv1 = new Vector3f();
				Vector3f tv2 = new Vector3f();
				Collections.sort(transparencies, new Comparator<Pair<Float, Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>>>>() {
					@Override
					public int compare(Pair<Float, Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>>> o1, Pair<Float, Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>>> o2) {
						Matrix4f m1 = o1.value2().value2().value1();
						Matrix4f m2 = o2.value2().value2().value1();
						Vector3f p1 = m1.getTranslation(tv1);
						Vector3f p2 = m2.getTranslation(tv2);
						Vector3f ca = Pipeline.this.getCamera().getPosition().toJoml();

						float d1 = p1.distanceSquared(ca);
						float d2 = p2.distanceSquared(ca);
						
						return (int) (d2-d1);
					}
				});
				
				// Draw them all
				for (int i = 0; i < transparencies.size(); i++) {
					Pair<Float, Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>>> t1 = transparencies.get(i);
					float alpha = t1.value1();
					Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>> t2 = t1.value2();
					RenderableMesh mesh = t2.value1();
					Pair<Matrix4f, MaterialGL> t3 = t2.value2();
					Matrix4f worldMatrix = t3.value1();
					MaterialGL material = t3.value2();
					
					BaseShader shader = this.shader_get();
					shader.shader_set_uniform_f(shader.shader_get_uniform("uTransparencyObject"), alpha);
					
					mesh.render(shader, worldMatrix, material);
				}
			}
		}
		
		tbuffer.unbind();
		
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
			
			// Draw final gbuffer composite
			drawTexture( gbuffer.getBufferFinal(), 0, 0, size.x, size.y );
			
			// Draw buffers
			if ( debug ) {
				int s = 150;
				drawTexture( gbuffer.getBuffer0(), s*0, 0, s, s );
				drawTexture( gbuffer.getBuffer1(), s*1, 0, s, s );
				drawTexture( gbuffer.getBuffer2(), s*2, 0, s, s );
				drawTexture( gbuffer.getAccumulationBuffer(), s*3, 0, s, s );
				drawTexture( tbuffer.getBuffer(), s*4, 0, s, s );
			}
			
			// Render final 3d overlay stuff
			{
				// Reset perspective projection
				perspective();
				shader_set(genericShader);
				
				// Render selected instances (handles)
				List<Instance> instances = Game.selectedExtended();
				HandlesRenderer.render(instances);
			}
		}
		buffer.unbind();
	}
	
	public GBuffer getGBuffer() {
		return this.gbuffer;
	}
	
	public TransparencyRenderer getTransparencyRenderer() {
		return this.tbuffer;
	}
	
	public void fullscreenQuad( ) {
		drawQuad( 0, 0, size.x, size.y);
	}
	
	private Matrix4f tempMat4 = new Matrix4f();
	private void drawQuad( int x, int y, int width, int height ) {
		fullscreenMesh.render(shader_get(), tempMat4.identity().translate(x, y, 0).scale(width, height,1), null);
	}
	
	private void drawTexture( int buffer, int x, int y, int width, int height) {
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		glBindTexture(GL11.GL_TEXTURE_2D, buffer);
		drawQuad( x, y, width, height );
	}
	
	private void drawTexture( Surface surface, int x, int y, int width, int height) {
		drawTexture(surface.getTextureId(),x,y,width,height);
	}
	
	public void addTransparentRenderableToQueue( RenderableInstance instance, float transparency ) {
		Prefab prefab = instance.getPrefab();
		if ( prefab != null ) {
			PrefabRenderer renderer = prefab.getPrefab();
			for (int i = 0; i < renderer.size(); i++) {
				Model model = renderer.getModel(i);

				Matrix4f worldMatrix = instance.getWorldMatrix().toJoml();
				worldMatrix.scale(prefab.getScale());
				
				addTransparentRenderableToQueue( model, worldMatrix, transparency );
			}
		}
	}
	
	public void addTransparentRenderableToQueue(Model model, Matrix4f worldMatrix, float transparency) {
		MaterialGL material = model.getMaterial().getMaterial();
		if ( material == null )
			material = Resources.MATERIAL_BLANK;
		
		BufferedMesh mesh = model.getMeshInternal();
		if ( mesh == null )
			mesh = Resources.MESH_SPHERE;
		
		Pair<Matrix4f, MaterialGL> t3 = new Pair<Matrix4f, MaterialGL>(worldMatrix, material);
		Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>> t2 = new Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>>(mesh, t3);
		Pair<Float, Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>>> p = new Pair<Float, Pair<RenderableMesh, Pair<Matrix4f, MaterialGL>>>(transparency, t2);
		transparencies.add(p);
	}
	
	/** Constant used to reference animation controller objects by class name */
	private static final LuaValue C_ANIMATIONCONTROLLER = LuaValue.valueOf("AnimationController");
	
	private void renderInstancesRecursive(BaseShader shader, Instance root) {
		List<Instance> instances = root.getChildren();
		for (int i = 0; i < instances.size(); i++) {
			Instance inst = instances.get(i);
			if ( inst instanceof Camera )
				continue;
			renderInstancesRecursive(shader, inst);
		}
		
		if ( root instanceof RenderableInstance ) {
			float transparency = 0;
			if ( root instanceof GameObject ) {
				GameObject g = ((GameObject)root);
				transparency = g.getTransparency();
			}
			
			// Replace shader with animatable shader
			Instance animationController = root.findFirstChildOfClass(C_ANIMATIONCONTROLLER);
			if ( animationController != null ) {
				((AnimationController)animationController).debugRender(shader);
				return;
			}
			
			if ( transparency == 0 ) { // Solid
				((RenderableInstance)root).render(shader);
			} else if ( transparency < 1 ) { // Partially transparent
				addTransparentRenderableToQueue( (RenderableInstance) root, transparency );
			} else { // Invisible
				
			}
		}
	}
	
	public void ortho() {
		ortho(size.x, size.y);
	}
	
	public void ortho(float width, float height) {
		projMatrix.set(IDENTITY).ortho(0, width, 0, height, -3200, 3200);
		viewMatrix.set(IDENTITY);
	}
	
	private void perspective() {
		Camera camera = renderableWorld.getCurrentCamera();
		if ( camera != null ) {
			float fov = camera.getFov();
			float aspect = (float)size.x/(float)size.y;
			
			// Set matrices
			viewMatrix.set(camera.getViewMatrix().getInternal());
			projMatrix.identity().perspective((float) Math.toRadians(fov), aspect, 0.1f, 512);
			currentCamera = camera;
		} else {
			viewMatrix.set(gbuffer.getViewMatrix());
			projMatrix.set(gbuffer.getProjectionMatrix());
			currentCamera = null;
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
		return (Pipeline) currentPipeline;
	}
	
	public static IPipeline pipeline_get_v2() {
		return currentPipeline;
	}
	
	public BaseShader shader_reset() {
		return shader_set(genericShader);
	}
	
	public BaseShader shader_get() {
		return this.currentShader;
	}
	
	@Override
	public IPointLightHandler getPointLightHandler() {
		return gbuffer.getLightProcessor().getPointLightHandler();
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
	 * Returns the camera object used to render the scene. Can be null, if no camera object was used in the current rendering functionality.
	 * @return
	 */
	public Camera getCamera() {
		return this.currentCamera;
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

	@Override
	public IDirectionalLightHandler getDirectionalLightHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDyamicSkybox(DynamicSkybox dynamicSkybox) {
		// TODO Auto-generated method stub
		
	}
}
