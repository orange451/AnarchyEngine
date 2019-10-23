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
in vec2 pixelSize;

out vec4 out_Color;

uniform sampler2D image;
uniform sampler2D previous;

uniform sampler2D gMotion;
uniform sampler2D gDepth;

uniform vec3 cameraPosition;
uniform vec3 previousCameraPosition;
uniform mat4 projectionMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform mat4 previousViewMatrix;

uniform bool useTAA;

void main() {
	out_Color = texture(image, textureCoords);
	out_Color.a = 1;
	if (!useTAA)
		return;
	vec3 neighbourhood[9];

	neighbourhood[0] = texture(image, textureCoords + vec2(-1, -1) * pixelSize).xyz;
	neighbourhood[1] = texture(image, textureCoords + vec2(+0, -1) * pixelSize).xyz;
	neighbourhood[2] = texture(image, textureCoords + vec2(+1, -1) * pixelSize).xyz;
	neighbourhood[3] = texture(image, textureCoords + vec2(-1, +0) * pixelSize).xyz;
	neighbourhood[4] = texture(image, textureCoords + vec2(+0, +0) * pixelSize).xyz;
	neighbourhood[5] = texture(image, textureCoords + vec2(+1, +0) * pixelSize).xyz;
	neighbourhood[6] = texture(image, textureCoords + vec2(-1, +1) * pixelSize).xyz;
	neighbourhood[7] = texture(image, textureCoords + vec2(+0, +1) * pixelSize).xyz;
	neighbourhood[8] = texture(image, textureCoords + vec2(+1, +1) * pixelSize).xyz;

	vec3 nmin = neighbourhood[0];
	vec3 nmax = neighbourhood[0];
	for (int i = 1; i < 9; ++i) {
		nmin = min(nmin, neighbourhood[i]);
		nmax = max(nmax, neighbourhood[i]);
	}

	float depthSample = texture(gDepth, textureCoords).r;
#ifdef OneToOneDepth
	vec4 currentPosition = vec4(textureCoords * 2.0 - 1.0, depthSample * 2.0 - 1.0, 1.0);
#else
	vec4 currentPosition = vec4(textureCoords * 2.0 - 1.0, depthSample, 1.0);
#endif
	vec4 fragposition = inverseProjectionMatrix * currentPosition;
	fragposition = inverseViewMatrix * fragposition;
	fragposition /= fragposition.w;

	vec4 previousPosition = fragposition;
	previousPosition = previousViewMatrix * previousPosition;
	previousPosition = projectionMatrix * previousPosition;
	previousPosition /= previousPosition.w;
	vec2 vel = (previousPosition - currentPosition).xy * 0.5;

	vel += texture(gMotion, textureCoords).rg * 0.5;

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