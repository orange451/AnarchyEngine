/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

layout(local_size_x = 16, local_size_y = 16) in ;
layout(rgba16f, binding = 0) restrict writeonly uniform image2D out_color;

uniform vec2 resolution;
uniform int frame;

uniform vec3 cameraPosition;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2D gNormal;
uniform sampler2D gMask;
uniform sampler2D gDepth;
uniform sampler2D gMotion;
uniform sampler2D directionalLightData;
uniform sampler2D pointLightData;
uniform sampler2D spotLightData;
uniform sampler2D areaLightData;
uniform sampler3D voxelImage;
uniform float voxelSize;
uniform float voxelOffset;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function getDepth

#include variable MASK

#include function randomS

#include function random

#include function cosWeightedRandomHemisphereDirection

vec3 roundStep(vec3 inp, float step) {
	return vec3((roundEven(inp.x / step)) * step, (roundEven(inp.y / step)) * step,
				(roundEven(inp.z / step)) * step);
}

// Random dir generator has issues

#define SAMPLES_GI 8
#define MAX_STEPS_GI 100
#define MAX_DIST_GI 20.0
#define SURF_DIST_GI 2.0
#define SAMPLE_STEP voxelOffset * 0.5

#define SAMPLES_AO 4
#define MAX_STEPS_AO 100
#define MAX_DIST_AO 1.5
#define SURF_DIST_AO 0.01

const bool useGI = true;

vec4 computeGI(vec2 textureCoords, vec3 position, vec3 normal, sampler2D gDepth) {
	vec4 giCombined = vec4(0.0);
	int j = 0;
	for (j = 0; j < SAMPLES_GI; j++) {
		vec4 gi = vec4(vec3(0.0), 1.0);
		float dO = 0.0;

		float rand2 = randomS(textureCoords + vec2(641.51224, 423.178), float(frame) + float(j));
		float rand3 = randomS(textureCoords - vec2(147.16414, 363.941), float(frame) - float(j));

		vec3 rd = cosWeightedRandomHemisphereDirection(normal, random(rand2), random(rand3));
		vec3 ro = position + rd * voxelOffset * SURF_DIST_GI;
		int i = 0;
		bool canHit = false;
		for (i = 0; i < MAX_STEPS_GI; i++) {

			vec3 p = ro + rd * dO;

			vec3 samplePos = (p.xyz - cameraPosition) / voxelSize + 0.5;

			if (samplePos.x < 0.0 || samplePos.x > 1.0 || samplePos.y < 0.0 || samplePos.y > 1.0 ||
				samplePos.z < 0.0 || samplePos.z > 1.0) {
				break;
			}

			vec4 samplePoint = texture(voxelImage, samplePos);

			if (samplePoint.a == 0) {
				canHit = true;
			}

			if (canHit && samplePoint.a > 0.0) {
				gi.rgb = samplePoint.rgb;
				gi.a = 0.0;
				break;
			}

			if (dO > MAX_DIST_GI) {
				break;
			}
			dO += SAMPLE_STEP;
		}
		giCombined += gi;
	}
	return giCombined / j;
}

void main() {
	ivec2 pixelcoords = ivec2(gl_GlobalInvocationID.xy);

	vec2 textureCoords = pixelcoords.xy / resolution;

	vec4 mask = texture(gMask, textureCoords);
	if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
		float depth = texture(gDepth, textureCoords).r;
		vec3 position =
			positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);

		vec2 xy = texture(gMotion, textureCoords).ba;
		vec4 normal = texture(gNormal, textureCoords);

		vec3 GIN = normalize(vec3(xy, normal.a));
		vec3 AON = normal.xyz;

		vec3 samplePos = (position - cameraPosition) / voxelSize + 0.5;

		if (!(samplePos.x < 0.0 || samplePos.x > 1.0 || samplePos.y < 0.0 || samplePos.y > 1.0 ||
			  samplePos.z < 0.0 || samplePos.z > 1.0)) {

			vec3 rd = normalize(position - cameraPosition);

			vec4 gi = vec4(vec3(0.0), 1.0);
			if (useGI) {
				gi = computeGI(textureCoords, position, GIN, gDepth);
			}
			imageStore(out_color, pixelcoords, gi);
		}
	}
}