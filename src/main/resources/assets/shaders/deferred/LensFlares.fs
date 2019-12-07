//
// This file is part of Light Engine
//
// Copyright (C) 2016-2019 Lux Vacuos
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

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