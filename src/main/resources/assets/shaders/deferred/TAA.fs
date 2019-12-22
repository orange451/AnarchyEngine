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
in vec2 pixelSize;

out vec4 out_Color;

uniform sampler2D image;
uniform sampler2D previous;

uniform sampler2D gMotion;

uniform bool useTAA;

void main() {
	out_Color.rgb = texture(image, textureCoords).rgb;
	out_Color.a = 0.0;
	if (!useTAA)
		return;
	vec3 neighbourhood[9];

	neighbourhood[0] = texture(image, textureCoords + vec2(-1, -1) * pixelSize).rgb;
	neighbourhood[1] = texture(image, textureCoords + vec2(+0, -1) * pixelSize).rgb;
	neighbourhood[2] = texture(image, textureCoords + vec2(+1, -1) * pixelSize).rgb;
	neighbourhood[3] = texture(image, textureCoords + vec2(-1, +0) * pixelSize).rgb;
	neighbourhood[4] = texture(image, textureCoords + vec2(+0, +0) * pixelSize).rgb;
	neighbourhood[5] = texture(image, textureCoords + vec2(+1, +0) * pixelSize).rgb;
	neighbourhood[6] = texture(image, textureCoords + vec2(-1, +1) * pixelSize).rgb;
	neighbourhood[7] = texture(image, textureCoords + vec2(+0, +1) * pixelSize).rgb;
	neighbourhood[8] = texture(image, textureCoords + vec2(+1, +1) * pixelSize).rgb;

	vec3 nmin = neighbourhood[0];
	vec3 nmax = neighbourhood[0];
	for (int i = 1; i < 9; ++i) {
		nmin = min(nmin, neighbourhood[i]);
		nmax = max(nmax, neighbourhood[i]);
	}

	vec2 vel = texture(gMotion, textureCoords).rg * 0.5;

	vec2 histUv = textureCoords + vel;

	vec3 histSample = clamp(texture(previous, histUv).rgb, nmin, nmax);

	float blend = 0.1;

	// 0.2 3x TAA
	// 0.1 4x TAA
	// 0.05 8x TAA

	bvec2 a = greaterThan(histUv, vec2(1.0, 1.0));
	bvec2 b = lessThan(histUv, vec2(0.0, 0.0));
	blend = any(bvec2(any(a), any(b))) ? 1.0 : blend;

	vec3 curSample = neighbourhood[4];
	out_Color.rgb = mix(histSample, curSample, vec3(blend));

	//	out_Color.rgb = vec3(vel.x, 0, vel.y);
	//out_Color.rgb = vec3(texture(gMotion, textureCoords).rg + 0.5, 0.0);
}