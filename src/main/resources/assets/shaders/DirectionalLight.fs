/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

#include struct DirectionalLight

in vec2 textureCoords;

out vec3 out_Color;

uniform vec3 cameraPosition;
uniform sampler2D gDiffuse;
uniform sampler2D gNormal;
uniform sampler2D gPBR; // R = roughness, G = metallic
uniform sampler2D gMask;
uniform sampler2D gDepth;
uniform bool useShadows;
uniform mat4 biasMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;

uniform DirectionalLight light;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlick

#include function fresnelSchlickRoughness

#include function computeShadowV2

#include function calcDirectionalLight

#include variable MASK

#define MAX_STEPS 100
#define MAX_DIST 1.0
#define SURF_DIST 0.01

float computeContactShadows(vec3 pos, vec3 N, vec3 L) {
	if (dot(N, L) < 0.0)
		return 1.0;
	float shadow = .0;

	vec3 newPos;
	vec4 newScreen;
	vec2 newCoords;

	float depth, newDepth;

	float tmpDepth;

	float dO = SURF_DIST;
	float odS;

	vec3 rd = L;
	vec3 ro = pos;
	int i = 0;
	bool cont = false;
	for (i = 0; i < MAX_STEPS; i++) {
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
		newPos = positionFromDepth(newCoords, tmpDepth, inverseProjectionMatrix, inverseViewMatrix);

		// Calculate point and new pos depths
		depth = length(newPos - cameraPosition);
		newDepth = length(p - cameraPosition);

		// Calculate distance from newPos to point
		float dS = min(length(newPos - p), 0.005);
		dO += max(dS, 0.001); // Add distance to distance from origin
		float diff = newDepth - depth;
		if (diff > 0.1) {
			shadow = 1.0;
			break;
		}
		if (diff >= 0.0) {
			shadow = 0.0;
			if (!cont) {
				cont = true;
				continue;
			} else {
				break;
			}
		}

		if (dO > MAX_DIST) {
			shadow = 1.0;
			break;
		}
	}
	if (i == MAX_STEPS) {
		shadow = 1.0;
	}
	return shadow;
}

#define SAMPLES_AO 1
#define MAX_STEPS_AO 100
#define MAX_DIST_AO 1.0
#define SURF_DIST_AO 0.01

float computeAmbientOcclusion(vec2 textureCoords, vec3 position, vec3 N, vec3 L, sampler2D gDepth,
							  mat4 projection, mat4 view, mat4 invProjection, mat4 invView) {
	if (dot(N, L) < 0.0)
		return 1.0;
	float aoCombined = 0.0;
	int j = 0;
	for (j = 0; j < SAMPLES_AO; j++) {
		float shadow = 1.0;

		vec3 newPos;
		vec4 newScreen;
		vec2 newCoords;
		vec3 newNorm;

		float depth, newDepth, diff, oldDist, tmpDepth;

		float dO = SURF_DIST_AO, odS, step = 0.01;

		vec3 rd = L;
		vec3 ro = position;
		int i = 0;
		bool hit = false;
		for (i = 0; i < MAX_STEPS_AO; i++) {
			// Move point
			vec3 p = ro + rd * dO;

			// Convert world to screen
			newScreen = view * vec4(p, 1);
			newScreen = projection * newScreen;
			newScreen /= newScreen.w;
			newCoords = newScreen.xy * 0.5 + 0.5;

			if (newCoords.x < 0 || newCoords.x > 1 || newCoords.y < 0 || newCoords.y > 1)
				break;

			// Get new pos
			tmpDepth = texture(gDepth, newCoords).r;
			newPos = positionFromDepth(newCoords, tmpDepth, invProjection, invView);
			//newNorm = texture(gNormal, newCoords).rgb;

			// Calculate point and new pos depths
			depth = length(newPos - cameraPosition);
			newDepth = length(p - cameraPosition);

			// Calculate distance from newPos to point
			float dS = min(length(newPos - p), step);

			diff = newDepth - depth;

			if (diff >= 0.05 && diff <= 0.1) {
				float halfD = oldDist / 2.0;
				//step = max(step / 2.0, 0.01);
				dS = -halfD;
			} else if (diff > 0.001 && diff < 0.05) {
				//if (dot(newNorm, normalize(p - ro)) < 0.5)
					hit = true;
				break;
			}

			dO += dS; // Add distance to distance from origin
			oldDist = dS;

			if (dO > MAX_DIST_AO)
				break;
		}
		if (hit) {
			shadow = smoothstep(0.05, 0.05001, diff);
		}
		aoCombined += shadow;
	}
	return aoCombined / j;
}

void main() {
	vec4 mask = texture(gMask, textureCoords);
	vec3 result = vec3(0.0);
	if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
		vec4 image = texture(gDiffuse, textureCoords);
		vec2 pbr = texture(gPBR, textureCoords).rg;
		float depth = texture(gDepth, textureCoords).r;
		vec3 position =
			positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);
		vec3 normal = texture(gNormal, textureCoords).rgb;
		float roughness = pbr.r;
		float metallic = pbr.g;

		vec3 N = normalize(normal);
		vec3 V = normalize(cameraPosition - position);

		vec3 F0 = vec3(0.04);
		F0 = mix(F0, image.rgb, metallic);

		//float contact = computeAmbientOcclusion(textureCoords, position, N, normalize(light.direction), gDepth, projectionMatrix, viewMatrix, inverseProjectionMatrix, inverseViewMatrix);
		result = calcDirectionalLight(light, position, image.rgb, N, V, F0, roughness, metallic);
	}
	out_Color = result;
}