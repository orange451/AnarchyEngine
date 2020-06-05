/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl;

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
import static org.lwjgl.opengl.GL11C.glClearColor;
import static org.lwjgl.opengl.GL11C.glClearDepth;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDepthFunc;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBClipControl;
import org.lwjgl.opengl.GL;

import engine.Game;
import engine.gl.lights.AreaLightHandler;
import engine.gl.lights.AreaLightInternal;
import engine.gl.lights.DirectionalLightHandler;
import engine.gl.lights.DirectionalLightInternal;
import engine.gl.lights.ILightHandler;
import engine.gl.lights.PointLightHandler;
import engine.gl.lights.PointLightInternal;
import engine.gl.lights.SpotLightHandler;
import engine.gl.lights.SpotLightInternal;
import engine.gl.pipeline.MultiPass;
import engine.gl.pipeline.PostProcess;
import engine.gl.renderers.AnimInstanceRenderer;
import engine.gl.renderers.InstanceRenderer;
import engine.gl.shaders.ShaderIncludes;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.lua.type.object.services.Lighting;
import engine.observer.RenderableWorld;
import ide.layout.windows.ErrorWindow;
import lwjgui.scene.Window;
import lwjgui.scene.layout.StackPane;

public class GLRenderer implements IPipeline {

	private boolean enabled;

	private EnvironmentRenderer envRenderer;
	private EnvironmentRenderer envRendererEntities;
	private IrradianceCapture irradianceCapture;
	private PreFilteredEnvironment preFilteredEnvironment;

	private SkyRenderer skyRenderer;
	private DynamicSkybox dynamicSkybox;

	private PointLightHandler pointLightHandler;
	private DirectionalLightHandler directionalLightHandler;
	private SpotLightHandler spotLightHandler;
	private AreaLightHandler areaLightHandler;

	private RenderingManager renderingManager;

	private HandlesRenderer handlesRenderer;

	private DeferredPipeline dp;
	private PostProcessPipeline pp;
	private RenderingSettings renderingSettings;
	private RendererData rnd;
	private IRenderingData rd;

	private VoxelizedManager vm;

	private Matrix4f projMatrix;
	private Camera currentCamera;
	private Sun sun;

	private GLResourceLoader loader;

	private RenderableWorld renderableWorld;

	private int width, height;
	private Vector2f size = new Vector2f();

	private boolean useARBClipControl = false;

	private boolean initialized;

	private Window window;

	public GLRenderer(Window window) {
		this.window = window;
		useARBClipControl = GL.getCapabilities().GL_ARB_clip_control;

		rnd = new RendererData();
		loader = new GLResourceLoader();
		sun = new Sun();
		rd = new IRenderingData();
		renderingSettings = new RenderingSettings();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

		ShaderIncludes.processIncludeFile("assets/shaders/includes/lighting.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/materials.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/common.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/global.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/color.isl");
	}

	@Override
	public void init() {
		try {
			width = window.getWidth();
			height = window.getHeight();

			renderingManager = new RenderingManager();

			envRenderer = new EnvironmentRenderer(64, false);
			envRendererEntities = new EnvironmentRenderer(128, true);
			irradianceCapture = new IrradianceCapture();
			rnd.irradianceCapture = irradianceCapture.getCubeTexture();
			preFilteredEnvironment = new PreFilteredEnvironment();
			rnd.brdfLUT = preFilteredEnvironment.getBRDFLUT();
			rnd.environmentMap = preFilteredEnvironment.getTexture();
			skyRenderer = new SkyRenderer(loader);
			renderingManager.addRenderer(new InstanceRenderer());
			renderingManager.addRenderer(new AnimInstanceRenderer());
			handlesRenderer = new HandlesRenderer();
			dp = new MultiPass(width, height);
			pp = new PostProcess(width, height, window.getContext().getNVG());
			projMatrix = Maths.createProjectionMatrix(width, height, 90, 0.1f, Float.POSITIVE_INFINITY, true);

			rnd.exposure = Game.lighting().getExposure();
			directionalLightHandler = new DirectionalLightHandler(width, height);
			pointLightHandler = new PointLightHandler(width, height);
			spotLightHandler = new SpotLightHandler(width, height);
			areaLightHandler = new AreaLightHandler(width, height);
			rnd.plh = pointLightHandler;
			rnd.dlh = directionalLightHandler;
			rnd.slh = spotLightHandler;
			rnd.alh = areaLightHandler;
			rnd.rs = renderingSettings;
			rnd.vm = vm = new VoxelizedManager();
			size.set(width, height);
			enabled = true;
			initialized = true;
		} catch (Exception e) {
			e.printStackTrace();
			new ErrorWindow("Error initializing renderer", true);
		}
		Game.userInputService().inputBeganEvent().connect((args) -> {
			if (args[0].get("KeyCode").eq_b(LuaValue.valueOf(GLFW.GLFW_KEY_F5))) {
				System.out.println("Reloading Shaders...");
				dp.reloadShaders();
				pp.reloadShaders();
			}
		});
	}

	private void shadowPass() {
		// TODO: Render transparent shadows using an extra texture
		if (renderingSettings.shadowsEnabled) {
			GPUProfiler.start("Shadow Pass");
			GPUProfiler.start("Directional");
			for (DirectionalLightInternal l : directionalLightHandler.getLights()) {
				if (!l.shadows || !l.visible)
					continue;
				l.setPosition(currentCamera.getPosition().getInternal());
				l.update();
				l.getShadowMap().bind();
				glClear(GL_DEPTH_BUFFER_BIT);
				renderingManager.renderShadow(l.getLightCamera());
				l.getShadowMap().unbind();
			}
			GPUProfiler.end();
			glCullFace(GL_FRONT);
			GPUProfiler.start("Spot");
			for (SpotLightInternal l : spotLightHandler.getLights()) {
				if (!l.shadows || !l.visible)
					continue;
				l.update();
				l.getShadowMap().bind();
				glClear(GL_DEPTH_BUFFER_BIT);
				renderingManager.renderShadow(l.getLightCamera());
				l.getShadowMap().unbind();
			}
			GPUProfiler.end();
			GPUProfiler.start("Point");
			for (PointLightInternal l : pointLightHandler.getLights()) {
				if (!l.shadows || !l.visible)
					continue;
				l.update();
				l.getShadowMap().bind();
				glClear(GL_DEPTH_BUFFER_BIT);
				renderingManager.renderShadow(l.getLightCamera());
				l.getShadowMap().unbind();
			}
			GPUProfiler.end();
			glCullFace(GL_BACK);
			GPUProfiler.end();
		}
	}

	private void environmentPass() {
		GPUProfiler.start("Environment Pass");
		GPUProfiler.start("Irradiance");
		GPUProfiler.start("CubeMap Render");
		envRenderer.renderIrradiance(skyRenderer, sun, rd, rnd);
		GPUProfiler.end();
		GPUProfiler.start("Irradiance Capture");
		irradianceCapture.render(envRenderer.getCubeTexture());
		GPUProfiler.end();
		GPUProfiler.end();
		GPUProfiler.start("Reflections");
		GPUProfiler.start("CubeMap Render");
		envRendererEntities.renderReflections(skyRenderer, sun, rd, rnd, renderingManager);
		GPUProfiler.end();
		GPUProfiler.start("PreFilteredEnvironment");
		preFilteredEnvironment.render(envRendererEntities.getCubeTexture());
		GPUProfiler.end();
		GPUProfiler.end();
		GPUProfiler.end();
	}

	private void occlusionPass() {
		glClear(GL_DEPTH_BUFFER_BIT);
	}

	private void voxelizePass() {
		renderingManager.renderVoxelize(rd, rnd);
	}

	private void gBufferPass() {
		GPUProfiler.start("G-Buffer pass");
		if (useARBClipControl) {
			ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_ZERO_TO_ONE);
			glDepthFunc(GL_GREATER);
			glClearDepth(0.0);
		}
		dp.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		GPUProfiler.start("RenderingManager");
		renderingManager.render(rd, rnd, size);
		GPUProfiler.end();
		GPUProfiler.start("Skybox");
		skyRenderer.render(rnd, rd, sun.getLight().direction, true, true);
		GPUProfiler.end();
		dp.unbind();
		if (useARBClipControl) {
			glClearDepth(1.0);
			glDepthFunc(GL_LESS);
			ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_NEGATIVE_ONE_TO_ONE);
		}
		GPUProfiler.end();
	}

	private void deferredPass() {
		GPUProfiler.start("Lighting");
		GPUProfiler.start("Directional");
		directionalLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
		GPUProfiler.end();
		GPUProfiler.start("Point");
		pointLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
		GPUProfiler.end();
		GPUProfiler.start("Spot");
		spotLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
		GPUProfiler.end();
		GPUProfiler.start("Area");
		areaLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
		GPUProfiler.end();
		GPUProfiler.end();
		GPUProfiler.start("Deferred Pass");
		dp.process(rnd, rd);
		GPUProfiler.end();
	}

	private void forwardPass() {
		GPUProfiler.start("Forward Pass");
		if (useARBClipControl) {
			ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_ZERO_TO_ONE);
			glDepthFunc(GL_GREATER);
			glClearDepth(0.0);
		}
		pp.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		dp.render(pp.getMain());
		GPUProfiler.start("RenderingManager");
		renderingManager.renderForward(rd, rnd);
		GPUProfiler.end();
		GPUProfiler.start("OutlineRendering");
		handlesRenderer.render(currentCamera, projMatrix, Game.selectedExtended(), size);
		GPUProfiler.end();
		pp.unbind();
		if (useARBClipControl) {
			glClearDepth(1.0);
			glDepthFunc(GL_LESS);
			ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_NEGATIVE_ONE_TO_ONE);
		}
		GPUProfiler.end();
	}

	private void postFXPass() {
		GPUProfiler.start("PostFX");
		pp.process(rnd, rd);
		GPUProfiler.end();
	}

	public RenderingSettings getRenderSettings() {
		return this.renderingSettings;
	}

	@Override
	public void render() {
		if (!enabled)
			return;
		if (this.renderableWorld == null)
			return;
		if (!Game.isLoaded())
			return;
		if (!initialized)
			return;

		currentCamera = renderableWorld.getCurrentCamera();
		if (currentCamera == null)
			return;
		resetState();

		// Update Projection
		Maths.createProjectionMatrix(projMatrix, this.width, this.height, currentCamera.getFov(), 0.1f,
				Float.POSITIVE_INFINITY, useARBClipControl);

		// Set global time for clouds
		sun.update(dynamicSkybox);

		//
		// mat4 mvpX = Ortho * glm::lookAt( vec3( 2, 0, 0 ), vec3( 0, 0, 0 ), vec3( 0,
		// 1, 0 ) );

		// Create an modelview-orthographic projection matrix see from +Y axis
		// mat4 mvpY = Ortho * glm::lookAt( vec3( 0, 2, 0 ), vec3( 0, 0, 0 ), vec3( 0,
		// 0, -1 ) );

		// Create an modelview-orthographic projection matrix see from +Z axis
		// mat4 mvpZ = Ortho * glm::lookAt( vec3( 0, 0, 2 ), vec3( 0, 0, 0 ), vec3( 0,
		// 1, 0 ) );

		// currentCamera.getViewMatrix().getInternal().set(spotLightHandler.getLights().get(0).getLightCamera().getViewMatrix());
		// currentCamera.getViewMatrixInternal().set(new Matrix4f().setLookAt(new
		// Vector3f(0, 0, 0), new Vector3f(-1,0,0), new Vector3f(0, 1, 0)));
		// currentCamera.getViewMatrixInternal().set(new Matrix4f().setLookAt(new
		// Vector3f(0, 0, 0), new Vector3f(0,-0,0), new Vector3f(0, 1, 0)));
		// currentCamera.getViewMatrixInternal().set(new Matrix4f().setLookAt(new
		// Vector3f(0, 0, 0), new Vector3f(0,0,-1), new Vector3f(0, 1, 0)));

		// currentCamera.getViewMatrixInternal().set(Maths.createViewMatrixRot(Math.toRadians(-90),
		// Math.toRadians(90), Math.toRadians(90), null));
		// currentCamera.getViewMatrixInternal().set(Maths.createViewMatrixRot(Math.toRadians(-90),
		// Math.toRadians(0), Math.toRadians(0), null));
		// currentCamera.getViewMatrixInternal().set(Maths.createViewMatrixRot(Math.toRadians(-180),
		// Math.toRadians(180), Math.toRadians(180), null));

		// Update lighting data
		Lighting lighting = Game.lighting();
		if (lighting != null && !lighting.isDestroyed()) {
			rnd.ambient = lighting.getAmbient().toJOML();
			rnd.exposure = lighting.getExposure();
			rnd.gamma = lighting.getGamma();
			rnd.saturation = lighting.getSaturation();
		}

		rd.camera = currentCamera;
		rd.projectionMatrix = projMatrix;

		GPUProfiler.startFrame();

		renderingManager.preProcess(renderableWorld.getInstance());
		shadowPass();
		environmentPass();
		// occlusionPass();
		voxelizePass();
		gBufferPass();
		deferredPass();
		forwardPass();
		postFXPass();
		renderingManager.end();

		pp.render();

		GPUProfiler.endFrame();

		rnd.previousViewMatrix.set(currentCamera.getViewMatrixInternal());
		rnd.previousProjectionMatrix.set(projMatrix);
	}

	public void setSize(int width, int height) {
		if (!enabled)
			return;
		if (width <= 2 || height <= 2)
			return;
		if (this.width == width && this.height == height)
			return;
		this.width = width;
		this.height = height;
		this.size.set(width, height);
		dp.resize(width, height);
		pp.resize(width, height);
		directionalLightHandler.resize(width, height);
		pointLightHandler.resize(width, height);
		spotLightHandler.resize(width, height);
		areaLightHandler.resize(width, height);
	}

	@Override
	public void dispose() {
		envRenderer.dispose();
		envRendererEntities.dispose();
		dp.dispose();
		pp.dispose();
		directionalLightHandler.dispose();
		pointLightHandler.dispose();
		spotLightHandler.dispose();
		areaLightHandler.dispose();
		skyRenderer.dispose();
		irradianceCapture.dispose();
		preFilteredEnvironment.dispose();
		renderingManager.dispose();
		handlesRenderer.dispose();
		vm.dispose();
	}

	@Override
	public void setRenderableWorld(RenderableWorld instance) {
		this.renderableWorld = instance;
	}

	@Override
	public RenderableWorld getRenderableWorld() {
		return this.renderableWorld;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public StackPane getDisplayPane() {
		return pp.getDisplayPane();
	}

	@Override
	public ILightHandler<PointLightInternal> getPointLightHandler() {
		return pointLightHandler;
	}

	@Override
	public ILightHandler<DirectionalLightInternal> getDirectionalLightHandler() {
		return directionalLightHandler;
	}

	@Override
	public ILightHandler<SpotLightInternal> getSpotLightHandler() {
		return spotLightHandler;
	}

	@Override
	public ILightHandler<AreaLightInternal> getAreaLightHandler() {
		return areaLightHandler;
	}

	@Override
	public void setDyamicSkybox(DynamicSkybox dynamicSkybox) {
		this.dynamicSkybox = dynamicSkybox;
		skyRenderer.setDynamicSky(dynamicSkybox);
		if (dynamicSkybox != null)
			directionalLightHandler.addLight(sun.addLight());
		else
			directionalLightHandler.removeLight(sun.removeLight());
	}

	@Override
	public void setStaticSkybox(Skybox skybox) {
		skyRenderer.setStaticSky(skybox);
	}

	@Override
	public void reloadStaticSkybox() {
		skyRenderer.reloadStaticSkybox();
	}

	public void resetState() {
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glClearColor(0.0f, 0, 0, 0.0f);
	}

	@Override
	public boolean isInitialized() {
		return this.initialized;
	}

}
