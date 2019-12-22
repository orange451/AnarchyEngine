/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

in vec2 textureCoords;
in vec4 posPos;

out vec3 out_Color;

uniform sampler2D image;
uniform vec2 resolution;

uniform int useFXAA;

#define FxaaInt2 ivec2
#define FxaaFloat2 vec2
#define FxaaTexLod0(t, p) textureLod(t, p, 0.0)
#define FxaaTexOff(t, p, o, r) textureLodOffset(t, p, 0.0, o)

#define FXAA_REDUCE_MIN (1.0 / 128.0)
#define FXAA_REDUCE_MUL (1.0 / 8.0)
#define FXAA_SPAN_MAX 8.0

#define rt_w resolution.x
#define rt_h resolution.y

vec3 FxaaPixelShader(vec4 posPos, sampler2D tex, vec2 rcpFrame) {
	vec3 rgbNW = FxaaTexLod0(tex, posPos.zw).xyz;
	vec3 rgbNE = FxaaTexOff(tex, posPos.zw, FxaaInt2(1, 0), rcpFrame.xy).xyz;
	vec3 rgbSW = FxaaTexOff(tex, posPos.zw, FxaaInt2(0, 1), rcpFrame.xy).xyz;
	vec3 rgbSE = FxaaTexOff(tex, posPos.zw, FxaaInt2(1, 1), rcpFrame.xy).xyz;
	vec3 rgbM = FxaaTexLod0(tex, posPos.xy).xyz;

	vec3 luma = vec3(0.299, 0.587, 0.114);
	float lumaNW = dot(rgbNW, luma);
	float lumaNE = dot(rgbNE, luma);
	float lumaSW = dot(rgbSW, luma);
	float lumaSE = dot(rgbSE, luma);
	float lumaM = dot(rgbM, luma);

	float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
	float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

	vec2 dir;
	dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
	dir.y = ((lumaNW + lumaSW) - (lumaNE + lumaSE));

	float dirReduce =
		max((lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL), FXAA_REDUCE_MIN);
	float rcpDirMin = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);
	dir = min(FxaaFloat2(FXAA_SPAN_MAX, FXAA_SPAN_MAX),
			  max(FxaaFloat2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX), dir * rcpDirMin)) *
		  rcpFrame.xy;

	vec3 rgbA = (1.0 / 2.0) * (FxaaTexLod0(tex, posPos.xy + dir * (1.0 / 3.0 - 0.5)).xyz +
							   FxaaTexLod0(tex, posPos.xy + dir * (2.0 / 3.0 - 0.5)).xyz);
	vec3 rgbB = rgbA * (1.0 / 2.0) +
				(1.0 / 4.0) * (FxaaTexLod0(tex, posPos.xy + dir * (0.0 / 3.0 - 0.5)).xyz +
							   FxaaTexLod0(tex, posPos.xy + dir * (3.0 / 3.0 - 0.5)).xyz);
	float lumaB = dot(rgbB, luma);
	if ((lumaB < lumaMin) || (lumaB > lumaMax))
		return rgbA;
	return rgbB;
}

vec3 PostFX(sampler2D tex, vec2 uv) {
	vec2 rcpFrame = vec2(1.0 / rt_w, 1.0 / rt_h);
	return FxaaPixelShader(posPos, tex, rcpFrame);
}

void main() {
	if (useFXAA == 1)
		out_Color = PostFX(image, textureCoords);
	else
		out_Color = texture(image, textureCoords).rgb;
}