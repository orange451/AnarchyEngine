/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2;

import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_GREATER;
import static org.lwjgl.opengl.GL11C.GL_LESS;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glClearDepth;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDepthFunc;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBClipControl;

import engine.application.RenderableApplication;
import engine.gl.Surface;
import engine.glv2.entities.Sun;
import engine.glv2.entities.SunCamera;
import engine.glv2.pipeline.MultiPass;
import engine.glv2.pipeline.PostProcess;
import engine.glv2.shaders.ShaderIncludes;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.EnvironmentRenderer;
import engine.glv2.v2.IRenderingData;
import engine.glv2.v2.IrradianceCapture;
import engine.glv2.v2.PostProcessPipeline;
import engine.glv2.v2.lights.DirectionalLightShadowMap;
import engine.lua.type.object.insts.Camera;
import engine.observer.Renderable;
import engine.observer.RenderableWorld;

public class GLRenderer implements Renderable {

	private boolean enabled;

	private EnvironmentRenderer envRenderer;
	private EnvironmentRenderer envRendererEntities;
	private IrradianceCapture irradianceCapture;
	private PreFilteredEnvironment preFilteredEnvironment;

	// private ParticleRenderer particleRenderer;
	private SkydomeRenderer skydomeRenderer;
	// private WaterRenderer waterRenderer;
	// private LightRenderer lightRenderer;
	// private RenderingManager renderingManager;

	private DirectionalLightShadowMap dlsm;

	private DeferredPipeline dp;
	private PostProcessPipeline pp;

	/*
	 * private IShadowPass shadowPass = (a) -> { }; private IGBufferPass gbufferPass
	 * = (a) -> { }; private IForwardPass forwardPass = (a, b) -> { };
	 */

	private float exposure = 1;

	private Matrix4f projMatrix;
	private Camera currentCamera;
	private Sun sun;

	private RenderingSettings rs;

	private RendererData rnd;

	private GLResourceLoader loader;

	private IRenderingData rd;
	
	private RenderableWorld renderableWorld;
	
	private int width, height;
	
	private float time = 12000, globalTime;

	public GLRenderer() {
	
		rnd = new RendererData();
		loader = new GLResourceLoader();
		sun = new Sun();
		rd = new IRenderingData();
		rs = new RenderingSettings();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		
		ShaderIncludes.processIncludeFile("assets/shaders/includes/lighting.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/materials.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/common.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/global.isl");
		
		init();
	}

	public void init() {
		if (enabled)
			return;

		// renderingManager = new RenderingManager();
		// lightRenderer = new LightRenderer();
		// frustum = new Frustum();

		envRenderer = new EnvironmentRenderer(32);
		envRendererEntities = new EnvironmentRenderer(128);
		irradianceCapture = new IrradianceCapture();
		rnd.irradianceCapture = irradianceCapture.getCubeTexture();
		preFilteredEnvironment = new PreFilteredEnvironment(loader.createEmptyCubeMap(128, true, true), loader);
		rnd.brdfLUT = preFilteredEnvironment.getBRDFLUT();
		rnd.environmentMap = preFilteredEnvironment.getCubeMapTexture();
		// particleRenderer = new ParticleRenderer(loader);
		skydomeRenderer = new SkydomeRenderer(loader);
		// waterRenderer = new WaterRenderer(loader);
		// renderingManager.addRenderer(new EntityRenderer());
		rnd.dlsm = dlsm = new DirectionalLightShadowMap(rs.shadowsResolution);
		dp = new MultiPass(RenderableApplication.windowWidth, RenderableApplication.windowHeight);
		pp = new PostProcess(RenderableApplication.windowWidth, RenderableApplication.windowHeight);
		width = RenderableApplication.windowWidth;
		height = RenderableApplication.windowHeight;
		projMatrix = Maths.createProjectionMatrix(RenderableApplication.windowWidth, RenderableApplication.windowHeight,
				90, 0.1f, Float.POSITIVE_INFINITY, true);

		// rnd.lights = lightRenderer.getLights();
		rnd.exposure = exposure;

		enabled = true;
	}

	private void shadowPass() {
		// TODO: Render transparent shadows using an extra texture
		SunCamera sunCamera = sun.getCamera();
		if (rs.shadowsEnabled) {
			sunCamera.switchProjectionMatrix(0);
			// frustum.calculateFrustum(sunCamera);

			dlsm.bind();
			dlsm.swapTexture(0);
			glClear(GL_DEPTH_BUFFER_BIT);
			// renderingManager.renderShadow(sunCamera);
			// shadowPass.shadowPass(sunCamera);

			sunCamera.switchProjectionMatrix(1);
			// frustum.calculateFrustum(sunCamera);

			dlsm.swapTexture(1);
			glClear(GL_DEPTH_BUFFER_BIT);
			// renderingManager.renderShadow(sunCamera);
			// shadowPass.shadowPass(sunCamera);

			sunCamera.switchProjectionMatrix(2);
			// frustum.calculateFrustum(sunCamera);

			dlsm.swapTexture(2);
			glClear(GL_DEPTH_BUFFER_BIT);
			// renderingManager.renderShadow(sunCamera);
			// shadowPass.shadowPass(sunCamera);

			sunCamera.switchProjectionMatrix(3);
			// frustum.calculateFrustum(sunCamera);

			dlsm.swapTexture(3);
			glClear(GL_DEPTH_BUFFER_BIT);
			// renderingManager.renderShadow(sunCamera);
			// shadowPass.shadowPass(sunCamera);
			dlsm.unbind();
			glCullFace(GL_FRONT);
			/*
			 * for (Light light : lightRenderer.getLights()) { if (light.useShadows()) { //
			 * frustum.calculateFrustum(light.getCamera()); light.getShadowMap().bind();
			 * glClear(GL_DEPTH_BUFFER_BIT);
			 * //renderingManager.renderShadow(light.getCamera());
			 * light.getShadowMap().unbind(); } }
			 */
			glCullFace(GL_BACK);
		}
	}

	private void environmentPass() {
		envRenderer.renderEnvironmentMap(currentCamera.getPosition().getInternal(), skydomeRenderer,
				sun.getSunPosition());
		irradianceCapture.render(envRenderer.getCubeTexture());
		// envRendererEntities.renderEnvironmentMap(currentCamera.getPosition().getInternal(),
		// skydomeRenderer, renderingManager, rd, rnd);
		preFilteredEnvironment.render(envRenderer.getCubeTexture().getTexture()); // Use entities here
	}

	private void occlusionPass() {
		// frustum.calculateFrustum(camera);
		glClear(GL_DEPTH_BUFFER_BIT);
	}

	private void gBufferPass() {

		ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_ZERO_TO_ONE);
		dp.bind();
		glDepthFunc(GL_GREATER);
		glClearDepth(0.0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		// gbufferPass.gBufferPass(camera);
		// renderingManager.render(camera);
		// waterRenderer.render(rd.getEngine().getEntitiesFor(waterFam), camera,
		// worldSimulation.getGlobalTime(), frustum);
		skydomeRenderer.render(currentCamera, projMatrix, /* worldSimulation, */ sun.getSunPosition(), true, true);
		glClearDepth(1.0);
		glDepthFunc(GL_LESS);
		dp.unbind();
		ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_NEGATIVE_ONE_TO_ONE);
	}

	private void deferredPass() {
		dp.process(rs, rnd, rd);
	}

	private void forwardPass() {
		ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_ZERO_TO_ONE);
		pp.bind();
		glDepthFunc(GL_GREATER);
		glClearDepth(0.0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		dp.render(pp.getMain());
		// forwardPass.forwardPass(rd, rnd);
		// particleRenderer.render(ParticleDomain.getParticles(), camera);
		// renderingManager.renderForward(rd, rnd);
		glClearDepth(1.0);
		glDepthFunc(GL_LESS);
		pp.unbind();
		ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_NEGATIVE_ONE_TO_ONE);
	}

	private void postFXPass() {
		pp.process(rs, rnd, rd);
	}

	@Override
	public void render() {
		if (!enabled)
			return;
		
		if ( this.renderableWorld == null )
			return;
		resetState();
		// renderingManager.preProcess(rd.getEngine().getEntitiesFor(renderable));
		currentCamera = renderableWorld.getCurrentCamera();
		
		this.time += 0.016f * 1;
		this.time %= 24000;
		// Set global time for clouds
		this.globalTime += 0.016f * 1;
		float res = time * 0.015f;
		sun.update(res, 0);
		System.out.println(time);
		
		rd.camera = currentCamera;
		rd.sun = sun;
		rd.projectionMatrix = projMatrix;

		shadowPass();

		environmentPass();

		// occlusionPass();

		gBufferPass();

		deferredPass();

		forwardPass();

		postFXPass();

		// renderingManager.end();

		pp.render();

		rnd.previousCameraPosition.set(currentCamera.getPosition().getInternal());
		rnd.previousViewMatrix.set(currentCamera.getViewMatrix().getInternal());

		/*
		 * if (window.getKeyboardHandler().isKeyPressed(GLFW.GLFW_KEY_F2)) {
		 * window.getKeyboardHandler().ignoreKeyUntilRelease(GLFW.GLFW_KEY_F2);
		 * System.out.println("Reloading Shaders..."); dp.reloadShaders();
		 * pp.reloadShaders(); }
		 */
	}

	public void setSize(int width, int height) {
		if (!enabled)
			return;
		if(this.width == width && this.height == height)
			return;
		this.width = width;
		this.height = height;
		dp.resize(width, height);
		pp.resize(width, height);
		// EventSubsystem.triggerEvent("lightengine.renderer.postresize");
		// if (surface != null)
		// surface.setImage(pp.getNVGTexture());
	}

	public void dispose() {
		if (!enabled)
			return;
		enabled = false;
		envRenderer.dispose();
		envRendererEntities.dispose();
		dlsm.dispose();
		dp.dispose();
		pp.dispose();
		// particleRenderer.cleanUp();
		irradianceCapture.dispose();
		preFilteredEnvironment.dispose();
		// waterRenderer.dispose();
		// renderingManager.dispose();
		// lightRenderer.dispose();
	}
	
	public void setRenderableWorld(RenderableWorld instance) {
		this.renderableWorld = instance;
	}
	
	public RenderableWorld getRenderableWorld() {
		return this.renderableWorld;
	}
	
	public void setEnabled( boolean enabled ) {
		this.enabled = enabled;
	}
	
	public Surface getPipelineBuffer() {
		return pp.getFinalSurface();
	}

	public void resetState() {
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}

	/*
	 * @Override public void setShadowPass(IShadowPass shadowPass) { this.shadowPass
	 * = shadowPass; }
	 * 
	 * @Override public void setGBufferPass(IGBufferPass gbufferPass) {
	 * this.gbufferPass = gbufferPass; }
	 * 
	 * @Override public void setForwardPass(IForwardPass forwardPass) {
	 * this.forwardPass = forwardPass; }
	 * 
	 * @Override public void setSurface(RendererSurface surface) { if (this.surface
	 * != null) this.surface.setImage(-1); this.surface = surface; if (enabled)
	 * this.surface.setImage(pp.getNVGTexture()); }
	 * 
	 * @Override public Frustum getFrustum() { return frustum; }
	 * 
	 * @Override public LightRenderer getLightRenderer() { return lightRenderer; }
	 */

}
