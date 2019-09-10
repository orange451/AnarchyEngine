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

package engine.glv2.pipeline;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE6;

import engine.glv2.RendererData;
import engine.glv2.objects.Texture;
import engine.glv2.pipeline.shaders.LocalLightsShader;
import engine.glv2.v2.DeferredPass;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.IRenderingData;

public class LocalLightsPass extends DeferredPass<LocalLightsShader> {

	public LocalLightsPass() {
		super("LocalLightsPass");
	}

	@Override
	protected LocalLightsShader setupShader() {
		return new LocalLightsShader(name);
	}

	@Override
	protected void setupShaderData(RendererData rnd, IRenderingData rd, LocalLightsShader shader) {
		shader.loadCameraData(rd.camera, rd.projectionMatrix);
		shader.loadPointLights(rnd.plh.getLights());
	}

	@Override
	protected void setupTextures(RendererData rnd, DeferredPipeline dp, Texture[] auxTex) {
		super.activateTexture(GL_TEXTURE0, GL_TEXTURE_2D, dp.getDiffuseTex().getTexture());
		super.activateTexture(GL_TEXTURE1, GL_TEXTURE_2D, dp.getPositionTex().getTexture());
		super.activateTexture(GL_TEXTURE2, GL_TEXTURE_2D, dp.getNormalTex().getTexture());
		super.activateTexture(GL_TEXTURE3, GL_TEXTURE_2D, dp.getDepthTex().getTexture());
		super.activateTexture(GL_TEXTURE4, GL_TEXTURE_2D, dp.getPbrTex().getTexture());
		super.activateTexture(GL_TEXTURE5, GL_TEXTURE_2D, dp.getMaskTex().getTexture());
		super.activateTexture(GL_TEXTURE6, GL_TEXTURE_2D, auxTex[0].getTexture());
		/*
		 * for (int x = 0; x < rnd.lights.size(); x++) { Light l = rnd.lights.get(x); if
		 * (l.useShadows()) { super.activateTexture(GL_TEXTURE7 + x, GL_TEXTURE_2D,
		 * l.getShadowMap().getShadowMap().getTexture()); } }
		 */
	}
	/*
	 * private FBO fbos[];
	 * 
	 * public LocalLightsPass(String name, int width, int height) { super(name,
	 * width, height); }
	 * 
	 * @Override public void init() { fbos = new FBO[2]; fbos[0] = new FBO(width,
	 * height, GL_RGBA16F, GL_RGBA, GL_FLOAT); fbos[1] = new FBO(width, height,
	 * GL_RGBA16F, GL_RGBA, GL_FLOAT); shader = new DeferredShadingShader(name);
	 * shader.start(); shader.loadResolution(new Vector2f(width, height));
	 * shader.stop(); }
	 * 
	 * 
	 * public void render(FBO[] auxs, IDeferredPipeline pipe, CubeMapTexture
	 * irradianceCapture, CubeMapTexture environmentMap, Texture brdfLUT, ShadowFBO
	 * shadow, List<Light> lights) { glActiveTexture(GL_TEXTURE0);
	 * glBindTexture(GL_TEXTURE_2D, pipe.getMainFBO().getDiffuseTex());
	 * glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D,
	 * pipe.getMainFBO().getPositionTex()); glActiveTexture(GL_TEXTURE2);
	 * glBindTexture(GL_TEXTURE_2D, pipe.getMainFBO().getNormalTex());
	 * glActiveTexture(GL_TEXTURE3); glBindTexture(GL_TEXTURE_2D,
	 * pipe.getMainFBO().getDepthTex()); glActiveTexture(GL_TEXTURE4);
	 * glBindTexture(GL_TEXTURE_2D, pipe.getMainFBO().getPbrTex());
	 * glActiveTexture(GL_TEXTURE5); glBindTexture(GL_TEXTURE_2D,
	 * pipe.getMainFBO().getMaskTex()); glActiveTexture(GL_TEXTURE6);
	 * glBindTexture(GL_TEXTURE_2D, auxs[0].getTexture()); for (int x = 0; x <
	 * lights.size(); x++) { Light l = lights.get(x); if (l.isShadow()) { if
	 * (!l.isShadowMapCreated()) continue; glActiveTexture(GL_TEXTURE14 + x);
	 * glBindTexture(GL_TEXTURE_2D, l.getShadowMap().getShadowMap()); } } }
	 * 
	 * @Override public void process(CameraEntity camera, Sun sun, Matrix4f
	 * previousViewMatrix, Vector3f previousCameraPosition, IWorldSimulation
	 * clientWorldSimulation, List<Light> tLights, FBO[] auxs, IDeferredPipeline
	 * pipe, RawModel quad, CubeMapTexture irradianceCapture, CubeMapTexture
	 * environmentMap, Texture brdfLUT, ShadowFBO shadowFBO, float exposure) {
	 * RenderingSettings rs = GraphicalSubsystem.getRenderingSettings();
	 * List<List<Light>> totalLights = chopped(tLights, 18); for (List<Light> lights
	 * : totalLights) { FBO tmp = fbos[0]; fbos[0] = fbos[1]; fbos[1] = tmp;
	 * 
	 * fbos[0].begin(); shader.start(); shader.loadMotionBlurData(camera,
	 * previousViewMatrix, previousCameraPosition);
	 * shader.loadLightPosition(sun.getSunPosition(), sun.getInvertedSunPosition());
	 * shader.loadviewMatrix(camera); shader.loadSettings(rs.depthOfFieldEnabled,
	 * rs.fxaaEnabled, rs.motionBlurEnabled, rs.volumetricLightEnabled,
	 * rs.ssrEnabled, rs.ambientOcclusionEnabled, rs.shadowsDrawDistance,
	 * rs.chromaticAberrationEnabled, rs.lensFlaresEnabled, rs.shadowsEnabled);
	 * shader.loadExposure(exposure); shader.loadPointLightsPos(lights);
	 * shader.loadTime(clientWorldSimulation.getGlobalTime());
	 * shader.loadLightMatrix(sun.getCamera().getViewMatrix());
	 * shader.loadBiasMatrix(((SunCamera) sun.getCamera()).getProjectionArray());
	 * glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); render(auxs, pipe,
	 * irradianceCapture, environmentMap, brdfLUT, shadowFBO, lights);
	 * glDrawArrays(GL_TRIANGLE_STRIP, 0, quad.getVertexCount()); shader.stop();
	 * fbos[0].end();
	 * 
	 * auxs[0] = fbos[0]; } }
	 * 
	 * @Override public void resize(int width, int height) { this.width = width;
	 * this.height = height; fbos[0].dispose(); fbos[1].dispose(); fbos[0] = new
	 * FBO(width, height, GL_RGBA16F, GL_RGBA, GL_FLOAT); fbos[1] = new FBO(width,
	 * height, GL_RGBA16F, GL_RGBA, GL_FLOAT); shader.start();
	 * shader.loadResolution(new Vector2f(width, height)); shader.stop(); }
	 * 
	 * @Override public void dispose() { fbos[0].dispose(); fbos[1].dispose();
	 * shader.dispose(); }
	 * 
	 * private static <T> List<List<T>> chopped(List<T> list, final int L) {
	 * List<List<T>> parts = new ArrayList<List<T>>(); final int N = list.size();
	 * for (int i = 0; i < N; i += L) { parts.add(new ArrayList<T>(list.subList(i,
	 * Math.min(N, i + L)))); } return parts; }
	 */

}
