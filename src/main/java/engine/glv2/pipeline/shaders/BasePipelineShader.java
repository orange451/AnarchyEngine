/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
