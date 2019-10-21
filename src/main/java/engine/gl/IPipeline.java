package engine.gl;

import engine.glv2.v2.lights.IDirectionalLightHandler;
import engine.glv2.v2.lights.IPointLightHandler;
import engine.glv2.v2.lights.ISpotLightHandler;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.observer.Renderable;
import engine.observer.RenderableWorld;

public interface IPipeline extends Renderable {

	public void setRenderableWorld(RenderableWorld instance);

	public RenderableWorld getRenderableWorld();

	public void setEnabled(boolean enabled);

	public Surface getPipelineBuffer();

	public void setSize(int width, int height);

	public IPointLightHandler getPointLightHandler();

	public IDirectionalLightHandler getDirectionalLightHandler();

	public ISpotLightHandler getSpotLightHandler();

	public void setDyamicSkybox(DynamicSkybox dynamicSkybox);

	public void setStaticSkybox(Skybox skybox);

	public void reloadStaticSkybox();
}
