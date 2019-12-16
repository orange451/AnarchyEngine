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

package engine.glv2.pipeline.shaders;

import org.joml.Vector2f;

import static org.lwjgl.opengl.GL33C.*;

import engine.glv2.shaders.ShaderProgram;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformBoolean;
import engine.glv2.shaders.data.UniformInteger;
import engine.glv2.shaders.data.UniformVec2;
import engine.glv2.v2.RenderingSettings;

public class BasePipelineShader extends ShaderProgram {

	private UniformVec2 resolution = new UniformVec2("resolution");

	private UniformBoolean useFXAA = new UniformBoolean("useFXAA");
	private UniformBoolean useDOF = new UniformBoolean("useDOF");
	private UniformBoolean useMotionBlur = new UniformBoolean("useMotionBlur");
	private UniformBoolean useReflections = new UniformBoolean("useReflections");
	private UniformBoolean useVolumetricLight = new UniformBoolean("useVolumetricLight");
	private UniformBoolean useAmbientOcclusion = new UniformBoolean("useAmbientOcclusion");
	private UniformBoolean useChromaticAberration = new UniformBoolean("useChromaticAberration");
	private UniformBoolean useLensFlares = new UniformBoolean("useLensFlares");
	private UniformBoolean useShadows = new UniformBoolean("useShadows");
	private UniformBoolean useTAA = new UniformBoolean("useTAA");

	private UniformInteger frame = new UniformInteger("frame");

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/pipeline/DefaultPipeline.vs", GL_VERTEX_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		super.storeUniforms(resolution, useFXAA, useDOF, useMotionBlur, useReflections, useVolumetricLight,
				useAmbientOcclusion, useChromaticAberration, useLensFlares, useShadows, frame, useTAA);
	}

	public void loadSettings(RenderingSettings rs) {
		this.useDOF.loadBoolean(rs.depthOfFieldEnabled);
		this.useFXAA.loadBoolean(rs.fxaaEnabled);
		this.useMotionBlur.loadBoolean(rs.motionBlurEnabled);
		this.useVolumetricLight.loadBoolean(rs.volumetricLightEnabled);
		this.useReflections.loadBoolean(rs.ssrEnabled);
		this.useAmbientOcclusion.loadBoolean(rs.ambientOcclusionEnabled);
		this.useChromaticAberration.loadBoolean(rs.chromaticAberrationEnabled);
		this.useLensFlares.loadBoolean(rs.lensFlaresEnabled);
		this.useShadows.loadBoolean(rs.shadowsEnabled);
		this.useTAA.loadBoolean(rs.taaEnabled);
	}

	public void loadFrame(int frame) {
		this.frame.loadInteger(frame);
	}

	public void loadResolution(Vector2f res) {
		resolution.loadVec2(res);
	}

}
