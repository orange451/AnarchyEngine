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

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBClipControl;
import org.lwjgl.opengl.GL;

import engine.Game;
import engine.InternalRenderThread;
import engine.application.RenderableApplication;
import engine.gl.IPipeline;
import engine.gl.Pipeline;
import engine.gl.Surface;
import engine.gl.light.DirectionalLightInternal;
import engine.glv2.pipeline.MultiPass;
import engine.glv2.pipeline.PostProcess;
import engine.glv2.renderers.AnimInstanceRenderer;
import engine.glv2.renderers.InstanceRenderer;
import engine.glv2.shaders.ShaderIncludes;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.DynamicSkyRenderer;
import engine.glv2.v2.EnvironmentRenderer;
import engine.glv2.v2.HandlesRenderer;
import engine.glv2.v2.IRenderingData;
import engine.glv2.v2.IrradianceCapture;
import engine.glv2.v2.PostProcessPipeline;
import engine.glv2.v2.Sun;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.observer.RenderableWorld;

public class GLRenderer implements IPipeline {

	private boolean enabled;

	private EnvironmentRenderer envRenderer;
	private EnvironmentRenderer envRendererEntities;
	private IrradianceCapture irradianceCapture;
	private PreFilteredEnvironment preFilteredEnvironment;

	private DynamicSkyRenderer dynamicSkyRenderer;
	private PointLightHandler pointLightHandler;
	private DirectionalLightHandler directionalLightHandler;
	private RenderingManager renderingManager;
	private HandlesRenderer handlesRenderer;

	private DeferredPipeline dp;
	private PostProcessPipeline pp;

	private Matrix4f projMatrix;
	private Camera currentCamera;
	private Sun sun;
	private DynamicSkybox dynamicSkybox;

	private RenderingSettings renderingSettings;

	private RendererData rnd;

	private GLResourceLoader loader;

	private IRenderingData rd;

	private RenderableWorld renderableWorld;

	private int width, height;
	private Vector2f size = new Vector2f();

	private float globalTime = 0;

	private boolean useARBClipControl = false;

	public GLRenderer() {
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

		Game.userInputService().inputBeganEvent().connect((args) -> {
			if (args[0].get("KeyCode").eq_b(LuaValue.valueOf(GLFW.GLFW_KEY_F5))) {
				System.out.println("Reloading Shaders...");
				dp.reloadShaders();
				pp.reloadShaders();
			}
		});

		init();
	}

	public void init() {
		width = RenderableApplication.windowWidth;
		height = RenderableApplication.windowHeight;

		renderingManager = new RenderingManager();

		envRenderer = new EnvironmentRenderer(32);
		envRendererEntities = new EnvironmentRenderer(128);
		irradianceCapture = new IrradianceCapture();
		rnd.irradianceCapture = irradianceCapture.getCubeTexture();
		preFilteredEnvironment = new PreFilteredEnvironment(loader.createEmptyCubeMap(128, true, true), loader);
		rnd.brdfLUT = preFilteredEnvironment.getBRDFLUT();
		rnd.environmentMap = preFilteredEnvironment.getCubeMapTexture();
		dynamicSkyRenderer = new DynamicSkyRenderer(loader);
		renderingManager.addRenderer(new InstanceRenderer());
		renderingManager.addRenderer(new AnimInstanceRenderer());
		handlesRenderer = new HandlesRenderer();
		dp = new MultiPass(width, height);
		pp = new PostProcess(width, height);
		projMatrix = Maths.createProjectionMatrix(width, height, 90, 0.1f, Float.POSITIVE_INFINITY, true);

		rnd.exposure = Game.lighting().getExposure();
		directionalLightHandler = new DirectionalLightHandler(width, height);
		pointLightHandler = new PointLightHandler(width, height);
		rnd.plh = pointLightHandler;
		rnd.dlh = directionalLightHandler;
		rnd.rs = renderingSettings;
		size.set(RenderableApplication.windowWidth, RenderableApplication.windowHeight);
		enabled = true;
	}

	private void shadowPass() {
		// TODO: Render transparent shadows using an extra texture
		if (renderingSettings.shadowsEnabled) {
			GPUProfiler.start("Shadow Pass");
			synchronized (directionalLightHandler.getLights()) {
				for (DirectionalLightInternal l : directionalLightHandler.getLights()) {
					if (!l.shadows)
						continue;
					l.setPosition(currentCamera.getPosition().getInternal());
					l.update();
					l.getShadowMap().bind();
					glClear(GL_DEPTH_BUFFER_BIT);
					renderingManager.renderShadow(l.getLightCamera());
					l.getShadowMap().unbind();
				}
			}
			GPUProfiler.end();
		}
	}

	private void environmentPass() {
		GPUProfiler.start("Environment Pass");
		GPUProfiler.start("Irradiance");
		GPUProfiler.start("CubeMap Render");
		envRenderer.renderEnvironmentMap(currentCamera.getPosition().getInternal(), dynamicSkyRenderer,
				sun.getLight().direction, globalTime);
		GPUProfiler.end();
		GPUProfiler.start("Irradiance Capture");
		irradianceCapture.render(envRenderer.getCubeTexture());
		GPUProfiler.end();
		GPUProfiler.end();
		GPUProfiler.start("Reflections");
		GPUProfiler.start("CubeMap Render");
		envRendererEntities.renderEnvironmentMap(currentCamera.getPosition().getInternal(), dynamicSkyRenderer,
				sun.getLight().direction, renderingManager, rd, rnd, globalTime);
		GPUProfiler.end();
		GPUProfiler.start("PreFilteredEnvironment");
		preFilteredEnvironment.render(envRendererEntities.getCubeTexture().getTexture());
		GPUProfiler.end();
		GPUProfiler.end();
		GPUProfiler.end();
	}

	private void occlusionPass() {
		glClear(GL_DEPTH_BUFFER_BIT);
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
		renderingManager.render(rd, rnd);
		GPUProfiler.end();
		GPUProfiler.start("Skybox");
		dynamicSkyRenderer.render(currentCamera, projMatrix, sun.getLight().direction, true, true);
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
		directionalLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
		pointLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
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

	@Override
	public void render() {
		if (!enabled)
			return;
		if (this.renderableWorld == null)
			return;
		if (!Game.isLoaded())
			return;

		currentCamera = renderableWorld.getCurrentCamera();
		if (currentCamera == null)
			return;
		resetState();

		// Update Projection
		Maths.createProjectionMatrix(projMatrix, this.width, this.height, currentCamera.getFov(), 0.1f,
				Float.POSITIVE_INFINITY, useARBClipControl);

		// Set global time for clouds
		this.globalTime += InternalRenderThread.delta * 100;
		sun.update(dynamicSkybox);

		// Update lighting data
		rnd.ambient = Game.lighting().getAmbient().toJOML();
		rnd.exposure = Game.lighting().getExposure();
		rnd.gamma = Game.lighting().getGamma();
		rnd.saturation = Game.lighting().getSaturation();

		rd.camera = currentCamera;
		rd.projectionMatrix = projMatrix;

		GPUProfiler.startFrame();

		renderingManager.preProcess(renderableWorld.getInstance());
		shadowPass();
		environmentPass();
		// occlusionPass();
		gBufferPass();
		deferredPass();
		forwardPass();
		postFXPass();
		renderingManager.end();

		pp.render();

		GPUProfiler.endFrame();

		rnd.previousCameraPosition.set(currentCamera.getPosition().getInternal());
		rnd.previousViewMatrix.set(currentCamera.getViewMatrix().getInternal());
	}

	public void setSize(int width, int height) {
		if (!enabled)
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
	}

	public void dispose() {
		envRenderer.dispose();
		envRendererEntities.dispose();
		dp.dispose();
		pp.dispose();
		directionalLightHandler.dispose();
		pointLightHandler.dispose();
		dynamicSkyRenderer.dispose();
		// particleRenderer.cleanUp();
		irradianceCapture.dispose();
		preFilteredEnvironment.dispose();
		// waterRenderer.dispose();
		renderingManager.dispose();
		handlesRenderer.dispose();
		// lightRenderer.dispose();
	}

	@Override
	public void setRenderableWorld(RenderableWorld instance) {
		this.renderableWorld = instance;
		Pipeline.set(this, instance);
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
	public Surface getPipelineBuffer() {
		return pp.getFinalSurface();
	}

	@Override
	public IPointLightHandler getPointLightHandler() {
		return pointLightHandler;
	}

	@Override
	public IDirectionalLightHandler getDirectionalLightHandler() {
		return directionalLightHandler;
	}

	@Override
	public void setDyamicSkybox(DynamicSkybox dynamicSkybox) {
		this.dynamicSkybox = dynamicSkybox;
		dynamicSkyRenderer.setSky(dynamicSkybox);
		if (dynamicSkybox != null)
			directionalLightHandler.addLight(sun.getLight());
		else
			directionalLightHandler.removeLight(sun.getLight());
	}

	public void resetState() {
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glClearColor(0.0f, 0, 0, 0.0f);
	}

}
