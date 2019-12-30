/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec4 out_Color;

uniform sampler2D bloom;
uniform sampler2D lensColor;

uniform int useLensFlares;

const int ghosts = 5;
const float ghostDispersal = 0.4;
const float haloWidth = 0.47;
const float distortion = 1.5;

vec3 textureDistorted(sampler2D tex, vec2 texcoord, vec2 direction, vec3 distortion) {
	return vec3(texture(tex, texcoord + direction * distortion.r).r,
				texture(tex, texcoord + direction * distortion.g).g,
				texture(tex, texcoord + direction * distortion.b).b);
}

void main() {
	vec3 result = vec3(0.0);
	if (useLensFlares == 1) {
		vec2 texcoord = -textureCoords + vec2(1.0);
		vec2 ghostVec = (vec2(0.5) - texcoord) * ghostDispersal;
		vec2 texelSize = 1.0 / vec2(textureSize(bloom, 0));
		vec3 distortion = vec3(-texelSize.x * distortion, 0.0, texelSize.x * distortion);
		vec2 direction = normalize(ghostVec);
		for (int i = 0; i < ghosts; ++i) {
			vec2 offset = texcoord + ghostVec * float(i);
			float weight = length(vec2(0.5) - offset) / length(vec2(0.5));
			weight = pow(1.0 - weight, 10.0);
			result += textureDistorted(bloom, offset, direction, distortion) * weight;
		}
		vec2 haloVec = normalize(ghostVec) * haloWidth;
		float weight = length(vec2(0.5) - fract(texcoord + haloVec)) / length(vec2(0.5));
		weight = pow(1.0 - weight, 5.0);
		result += textureDistorted(bloom, texcoord + haloVec, direction, distortion) * weight;
		result *= texture(lensColor, vec2(length(vec2(0.5) - texcoord) / length(vec2(0.5)), 0.0)).rgb;
	}
	out_Color.rgb = result;
	out_Color.a = 0.0;
}