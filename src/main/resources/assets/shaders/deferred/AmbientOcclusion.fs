/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec4 out_Color;

uniform vec3 cameraPosition;
uniform sampler2D gNormal;
uniform sampler2D gMask;
uniform sampler2D gDepth;
uniform sampler2D gMotion;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2D directionalLightData;
uniform sampler2D pointLightData;
uniform sampler2D spotLightData;
uniform sampler2D areaLightData;
uniform sampler3D voxelImage;
uniform float time;
uniform float voxelSize;
uniform float voxelOffset;

uniform bool useAmbientOcclusion;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function getDepth

//#include function computeAmbientOcclusionV2

#include variable MASK

/*
  http://amindforeverprogramming.blogspot.de/2013/07/random-floats-in-glsl-330.html?showComment=1507064059398#c5427444543794991219
*/
uint hash3(uint x, uint y, uint z) {
	x += x >> 11;
	x ^= x << 7;
	x += y;
	x ^= x << 3;
	x += z ^ (x >> 14);
	x ^= x << 6;
	x += x >> 15;
	x ^= x << 5;
	x += x >> 12;
	x ^= x << 9;
	return x;
}

/*
  Generate a random value in [-1..+1)
*/
float randomS(vec2 pos, float time) {
	uint mantissaMask = 0x007FFFFFu;
	uint one = 0x3F800000u;
	uvec3 u = floatBitsToUint(vec3(pos, time));
	uint h = hash3(u.x, u.y, u.z);
	return uintBitsToFloat((h & mantissaMask) | one) - 1.0;
}

#include function random

vec3 cosWeightedRandomHemisphereDirection(vec3 n, float rand1, float rand2) {
	float Xi1 = rand1;
	float Xi2 = rand2;

	float theta = acos(sqrt(1.0 - Xi1));
	float phi = 2.0 * PI * Xi2;

	float xs = sin(theta) * cos(phi);
	float ys = cos(theta);
	float zs = sin(theta) * sin(phi);

	vec3 y = n;
	vec3 h = y;
	if (abs(h.x) <= abs(h.y) && abs(h.x) <= abs(h.z))
		h.x = 1.0;
	else if (abs(h.y) <= abs(h.x) && abs(h.y) <= abs(h.z))
		h.y = 1.0;
	else
		h.z = 1.0;

	vec3 x = normalize(cross(h, y));
	vec3 z = normalize(cross(x, y));

	vec3 direction = xs * x + ys * y + zs * z;
	return normalize(direction);
}

vec3 roundStep(vec3 inp, float step) {
	return vec3((roundEven(inp.x / step)) * step, (roundEven(inp.y / step)) * step,
				(roundEven(inp.z / step)) * step);
}

// Random dir generator has issues

#define SAMPLES_GI 4
#define MAX_STEPS_GI 100
#define MAX_DIST_GI 50.0
#define SURF_DIST_GI 2.0
#define SAMPLE_STEP 0.25

#define SAMPLES_AO 3
#define MAX_STEPS_AO 100
#define MAX_DIST_AO 1.5
#define SURF_DIST_AO 0.01

vec4 computeGI(vec2 texCoords, vec3 position, vec3 normal, sampler2D gDepth, mat4 projection,
			   mat4 invProjection, mat4 invView) {
	vec4 giCombined = vec4(0.0);
	int j = 0;
	for (j = 0; j < SAMPLES_GI; j++) {
		vec4 gi = vec4(vec3(0.0), 1.0);
		float dO = 0.0;

		float rand1 = randomS(textureCoords, time + float(j));
		float rand2 = randomS(textureCoords + vec2(641.51224, 423.178), time + float(j));
		float rand3 = randomS(textureCoords - vec2(147.16414, 363.941), time - float(j));
		vec3 rand = vec3(rand1, rand2, rand3);

		vec3 rd = cosWeightedRandomHemisphereDirection(normal, random(rand2), random(rand3));
		vec3 ro = position + rd * voxelOffset * SURF_DIST_GI;
		int i = 0;
		bool canHit = false;
		for (i = 0; i < MAX_STEPS_GI; i++) {

			vec3 p = ro + rd * dO;

			vec3 samplePos = roundStep(p.xyz - cameraPosition, voxelOffset) / voxelSize + 0.5;

			if (samplePos.x < 0.0 || samplePos.x > 1.0 || samplePos.y < 0.0 || samplePos.y > 1.0 ||
				samplePos.z < 0.0 || samplePos.z > 1.0) {
				break;
			}

			if (!canHit && i > 3)
				break;

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

float computeAmbientOcclusion(vec2 texCoords, vec3 position, vec3 normal, sampler2D gDepth,
							  mat4 projection, mat4 invProjection, mat4 invView) {
	float aoCombined = 0.0;
	int j = 0;
	for (j = 0; j < SAMPLES_AO; j++) {
		float shadow = 1.0;

		vec3 newPos;
		vec4 newScreen;
		vec2 newCoords;

		float depth, newDepth;

		float tmpDepth;

		float dO = 0.0;
		float odS;

		float rand1 = randomS(textureCoords, time + float(j));
		float rand2 = randomS(textureCoords + vec2(641.51224, 423.178), time + float(j));
		float rand3 = randomS(textureCoords - vec2(147.16414, 363.941), time - float(j));
		vec3 rand = vec3(rand1, rand2, rand3);

		vec3 rd = cosWeightedRandomHemisphereDirection(normal, random(rand2), random(rand3));
		vec3 ro = position + rd * SURF_DIST_AO;
		int i = 0;
		for (i = 0; i < MAX_STEPS_AO; i++) {
			// Move point
			vec3 p = ro + rd * dO;

			// Convert world to screen
			newScreen = viewMatrix * vec4(p, 1);
			newScreen = projectionMatrix * newScreen;
			newScreen /= newScreen.w;
			newCoords = newScreen.xy * 0.5 + 0.5;

			if (newCoords.x < 0 || newCoords.x > 1 || newCoords.y < 0 || newCoords.y > 1) {
				shadow = 1.0;
				break;
			}

			// Get new pos
			tmpDepth = texture(gDepth, newCoords).r;
			newPos =
				positionFromDepth(newCoords, tmpDepth, inverseProjectionMatrix, inverseViewMatrix);

			// Calculate point and new pos depths
			depth = length(newPos - cameraPosition);
			newDepth = length(p - cameraPosition);

			// Calculate distance from newPos to point
			float dS = min(length(newPos - p), 0.1);
			dO += dS; // Add distance to distance from origin

			float diff = newDepth - depth;

			if (diff > 0.001) {
				// shadow = smoothstep(0.0, 1.0, dO); // Ground truth returns 0.0
				shadow = smoothstep(1.0, 1.1, diff); // Ground truth returns 0.0
				break;
			}

			if (dO > MAX_DIST_AO) {
				shadow = 1.0;
				break;
			}
		}
		if (i == MAX_STEPS_AO) {
			shadow = 1.0;
		}
		aoCombined += shadow;
	}
	return aoCombined / j;
}

void main() {
	vec4 mask = texture(gMask, textureCoords);
	vec4 image = vec4(vec3(0.0), 1.0);
	if (useAmbientOcclusion) {
		if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
			float depth = texture(gDepth, textureCoords).r;
			vec3 position =
				positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);

			vec2 xy = texture(gMotion, textureCoords).ba;
			vec4 normal = texture(gNormal, textureCoords);

			vec3 GIN = normalize(vec3(xy, normal.a));
			vec3 AON = normal.xyz;

			vec3 samplePos = (position - cameraPosition) / voxelSize + 0.5;

			if (!(samplePos.x < 0.0 || samplePos.x > 1.0 || samplePos.y < 0.0 ||
				  samplePos.y > 1.0 || samplePos.z < 0.0 || samplePos.z > 1.0)) {

				vec3 rd = normalize(position - cameraPosition);

				/*vec3 cameraToWorld = position - cameraPosition;
				float cameraToWorldDist = length(cameraToWorld);

				vec3 rayTrace = cameraPosition;
				float rayDist, incr = 0.025;
				float rayDistRad = length(position - cameraPosition);
				do {
					rayTrace += rd * incr;
					incr *= 1.010;

					rayDist = length(rayTrace - cameraPosition);
					vec4 pointSample =
						texture(voxelImage, vec3((rayTrace.xyz - cameraPosition) / voxelSize +
				0.5)); if (pointSample.a > 0.0) { image.rgb = pointSample.rgb; break;
					}
				} while (rayDist < 200);*/

				vec4 gi = computeGI(textureCoords, position, GIN, gDepth, projectionMatrix,
									inverseProjectionMatrix, inverseViewMatrix);

				float ao =
					computeAmbientOcclusion(textureCoords, position, AON, gDepth, projectionMatrix,
											inverseProjectionMatrix, inverseViewMatrix);
				gi.a *= ao;
				gi.rgb *= ao;
				image = vec4(gi);
			}
		}
	}
	out_Color = image;
}