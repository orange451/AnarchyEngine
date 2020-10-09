/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.compute;

import org.joml.Vector2f;

import engine.gl.RenderingSettings;
import engine.gl.shaders.ShaderProgram;
import engine.gl.shaders.data.UniformBoolean;
import engine.gl.shaders.data.UniformInteger;
import engine.gl.shaders.data.UniformVec2;

public class BaseComputeShader extends ShaderProgram {

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
	private UniformVec2 pixelSize = new UniformVec2("pixelSize");
	private UniformVec2 pixelOffset = new UniformVec2("pixelOffset");

	@Override
	protected void setupShader() {
		super.storeUniforms(resolution, useFXAA, useDOF, useMotionBlur, useReflections, useVolumetricLight,
				useAmbientOcclusion, useChromaticAberration, useLensFlares, useShadows, frame, useTAA, pixelSize,
				pixelOffset);
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
		pixelSize.loadVec2(new Vector2f(1.0f).div(res));
		pixelOffset.loadVec2(new Vector2f(0.5f).div(res));
	}

}
