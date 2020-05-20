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
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2D directionalLightData;
uniform sampler2D pointLightData;
uniform sampler2D spotLightData;
uniform sampler2D areaLightData;

uniform bool useAmbientOcclusion;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function getDepth
#include function random

//#include function computeAmbientOcclusionV2

/*
  Generate a uniformly distributed random point on the unit-sphere.

  After:
  http://mathworld.wolfram.com/SpherePointPicking.html
 */
vec3 randomSpherePoint(vec3 rand) {
	float ang1 = (rand.x + 1.0) * PI; // [-1..1) -> [0..2*PI)
	float u = rand.y; // [-1..1), cos and acos(2v-1) cancel each other out, so we arrive at [-1..1)
	float u2 = u * u;
	float sqrt1MinusU2 = sqrt(1.0 - u2);
	float x = sqrt1MinusU2 * cos(ang1);
	float y = sqrt1MinusU2 * sin(ang1);
	float z = u;
	return vec3(x, y, z);
}

vec3 randomHemispherePoint(vec3 rand, vec3 n) {
	vec3 v = randomSpherePoint(rand);
	return v * sign(dot(v, n));
}

const float distanceThreshold = 0.5;
const int sample_count = 16;
const vec2 poisson16[] = vec2[](
	vec2(-0.94201624, -0.39906216), vec2(0.94558609, -0.76890725), vec2(-0.094184101, -0.92938870),
	vec2(0.34495938, 0.29387760), vec2(-0.91588581, 0.45771432), vec2(-0.81544232, -0.87912464),
	vec2(-0.38277543, 0.27676845), vec2(0.97484398, 0.75648379), vec2(0.44323325, -0.97511554),
	vec2(0.53742981, -0.47373420), vec2(-0.26496911, -0.41893023), vec2(0.79197514, 0.19090188),
	vec2(-0.24188840, 0.99706507), vec2(-0.81409955, 0.91437590), vec2(0.19984126, 0.78641367),
	vec2(0.14383161, -0.14100790));

#define SAMPLES_AO 3
#define MAX_STEPS_AO 100
#define MAX_DIST_AO 1.0
#define SURF_DIST_AO 0.01

#define SAMPLES_GI 3
#define MAX_STEPS_GI 100
#define MAX_DIST_GI 10.0
#define SURF_DIST_GI 0.01

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

		vec3 rand = vec3(random(position.x + j), random(position.y - j), 0);
		rand = rand * 2.0 - 1.0;
		vec3 rd = randomHemispherePoint(rand, normal);
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
				//shadow = smoothstep(0.0, 1.0, dO); // Ground truth returns 0.0
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

vec3 computeGI(vec2 texCoords, vec3 position, vec3 normal, sampler2D gDepth,
							  mat4 projection, mat4 invProjection, mat4 invView) {
	vec3 aoCombined = vec3(0.0);
	int j = 0;
	for (j = 0; j < SAMPLES_GI; j++) {
		vec3 shadow = vec3(0.0);

		vec3 newPos;
		vec4 newScreen;
		vec2 newCoords;
		vec3 newNorm;

		float depth, newDepth;

		float tmpDepth;

		float dO = 0.0;
		float odS;

		vec3 rand = vec3(random(position.x + j), random(position.y - j), 0);
		rand = rand * 2.0 - 1.0;
		vec3 rd = randomHemispherePoint(rand, normal);
		vec3 ro = position + rd * SURF_DIST_GI;
		int i = 0;
		for (i = 0; i < MAX_STEPS_GI; i++) {
			// Move point
			vec3 p = ro + rd * dO;

			// Convert world to screen
			newScreen = viewMatrix * vec4(p, 1);
			newScreen = projectionMatrix * newScreen;
			newScreen /= newScreen.w;
			newCoords = newScreen.xy * 0.5 + 0.5;

			if (newCoords.x < 0 || newCoords.x > 1 || newCoords.y < 0 || newCoords.y > 1) {
				shadow = vec3(0.0);
				break;
			}

			// Get new pos
			tmpDepth = texture(gDepth, newCoords).r;
			newPos =
				positionFromDepth(newCoords, tmpDepth, inverseProjectionMatrix, inverseViewMatrix);
			newNorm = texture(gNormal, newCoords).rgb;

			// Calculate point and new pos depths
			depth = length(newPos - cameraPosition);
			newDepth = length(p - cameraPosition);

			// Calculate distance from newPos to point
			float dS = min(length(newPos - p), 0.1);
			dO += dS; // Add distance to distance from origin

			float diff = newDepth - depth;

			if (diff > 0.001) {
				vec3 pMro = p - ro;
				float dist = length(pMro);
				if (dot(newNorm, normalize(pMro)) < 0.0) {
					shadow += texture(directionalLightData, newCoords).rgb; // Ground truth returns 0.0
					shadow += texture(pointLightData, newCoords).rgb; // Ground truth returns 0.0
					shadow += texture(spotLightData, newCoords).rgb; // Ground truth returns 0.0
					shadow += texture(areaLightData, newCoords).rgb; // Ground truth returns 0.0
					shadow *= smoothstep(1.1, 1.0, diff);
				}
				break;
			}

			if (dO > MAX_DIST_GI) {
				shadow = vec3(0.0);
				break;
			}
		}
		if (i == MAX_STEPS_GI) {
			shadow = vec3(0.0);
		}
		aoCombined += shadow;
	}
	return aoCombined / j;
}

#include variable MASK

void main() {
	vec4 mask = texture(gMask, textureCoords);
	vec4 image = vec4(1.0);
	if (useAmbientOcclusion) {

		if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
			float depth = texture(gDepth, textureCoords).r;
			vec3 position =
				positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);
			vec3 normal = texture(gNormal, textureCoords).rgb;
			vec3 N = normalize(normal);
			float ao = computeAmbientOcclusion(textureCoords, position, N, gDepth, projectionMatrix,
											   inverseProjectionMatrix, inverseViewMatrix);
			vec3 gi = computeGI(textureCoords, position, N, gDepth, projectionMatrix,
											   inverseProjectionMatrix, inverseViewMatrix);
			image = vec4(gi, ao);
		}
	}
	out_Color = image;
}