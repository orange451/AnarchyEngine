/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec4 out_Color;

uniform vec3 cameraPosition;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2D gDiffuse;
uniform sampler2D gNormal;
uniform sampler2D gPBR;
uniform sampler2D gMask;
uniform sampler2D gDepth;
uniform samplerCube environmentCube;
uniform sampler2D brdfLUT;
uniform sampler2D pass;
uniform sampler2D baseTex;		 // No reflection
uniform sampler2D reflectionTex; // Reflection data
uniform int frame;
uniform sampler3D voxelImage;
uniform float voxelSize;
uniform float voxelOffset;

uniform bool useReflections;

#include variable GLOBAL

#include function positionFromDepth

#include function fresnelSchlickRoughness

#include function getDepth

#include variable MASK

#include variable pi

#include function randomS

#include function random

#include function cosWeightedRandomHemisphereDirection

#define MAX_STEPS 100
#define MAX_DIST 100.0
#define SURF_DIST 0.01
#define SAMPLES 3

#define SAMPLES_GI 8
#define MAX_STEPS_GI 800
#define MAX_DIST_GI 800.0
#define SURF_DIST_GI 2.0
#define SAMPLE_STEP 0.5

vec4 computeGI(vec2 texCoords, vec3 position, vec3 normal, sampler2D gDepth, mat4 projection,
			   mat4 invProjection, mat4 invView, float roughness) {
	vec4 giCombined = vec4(0.0);
	int j = 0;
	for (j = 0; j < SAMPLES_GI; j++) {
		vec4 gi = vec4(vec3(0.0), 0.0);
		float dO = 0.0;

		float rand2 = randomS(textureCoords + vec2(641.51224, 423.178), float(frame) + float(j));
		float rand3 = randomS(textureCoords - vec2(147.16414, 363.941), float(frame) - float(j));
		
		vec3 rd = cosWeightedRandomHemisphereDirection(normal, random(rand2) * roughness * 0.10,
					random(rand3));
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
				gi.a = 1.0;
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

void main(void) {
	vec2 texcoord = textureCoords;
	vec4 image = vec4(0.0);
	vec4 mask = texture(gMask, texcoord);
	if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
		vec4 diffuse = texture(gDiffuse, textureCoords);
		vec2 pbr = texture(gPBR, textureCoords).rg;
		float frameDepth = texture(gDepth, textureCoords).r;
		vec3 position = positionFromDepth(textureCoords, frameDepth, inverseProjectionMatrix,
										  inverseViewMatrix);
		vec3 normal = texture(gNormal, textureCoords).rgb;

		vec3 N = normalize(normal);
		vec3 V = normalize(cameraPosition - position);
		vec3 R = reflect(-V, N);
		float ndotv = max(dot(N, V), 0.0);

		float roughness = pbr.r;
		float metallic = pbr.g;

		vec3 F0 = vec3(0.04);
		F0 = mix(F0, diffuse.rgb, metallic);
		vec3 F = fresnelSchlickRoughness(ndotv, F0, roughness);

		vec2 envBRDF = texture(brdfLUT, vec2(ndotv, roughness)).rg;

		vec3 reflectionMult = F * envBRDF.x + envBRDF.y;

		if (useReflections && roughness < 0.75) {
			vec3 camToWorld = position - cameraPosition.xyz;
			vec3 camToWorldNorm = normalize(camToWorld);
			vec3 combined = vec3(0.0);

			int samples = 0;
			for (int j = 0; j < SAMPLES; j++) {
				vec3 newPos;
				vec4 newScreen;
				vec2 newCoords;

				float depth, newDepth;

				float tmpDepth;

				float dO = 0.0;
				float odS;

				float rand2 =
					randomS(textureCoords + vec2(641.51224, 423.178), float(frame) + float(j));
				float rand3 =
					randomS(textureCoords - vec2(147.16414, 363.941), float(frame) - float(j));
				// hmmmmm, maybe uses different input data for angle
				vec3 rd = cosWeightedRandomHemisphereDirection(
					normalize(reflect(camToWorldNorm, N)), random(rand2) * roughness * 0.10,
					random(rand3));
				vec3 ro = position + rd * SURF_DIST;
				int i = 0;

				bool hit = false;
				vec3 newNorm;

				float oldDist = 0.0;

				for (i = 0; i < MAX_STEPS; i++) {
					// Move point
					vec3 p = ro + rd * dO;

					// Convert world to screen
					newScreen = viewMatrix * vec4(p, 1);
					newScreen = projectionMatrix * newScreen;
					newScreen /= newScreen.w;
					newCoords = newScreen.xy * 0.5 + 0.5;

					if (newCoords.x < 0 || newCoords.x > 1 || newCoords.y < 0 || newCoords.y > 1)
						break;

					// Get new pos
					tmpDepth = texture(gDepth, newCoords).r;
					newPos = positionFromDepth(newCoords, tmpDepth, inverseProjectionMatrix,
											   inverseViewMatrix);
					newNorm = texture(gNormal, newCoords).rgb;

					// Calculate point and new pos depths
					depth = length(newPos - cameraPosition);
					newDepth = length(p - cameraPosition);

					// Calculate distance from newPos to point
					float dS = min(length(newPos - p), 1.0);

					float diff = newDepth - depth;

					if (diff >= 0.2 && diff <= 2.0) {
						float halfD = oldDist / 2.0;
						dS = -halfD;
					} else if (diff > -0.001 && diff < 0.2) {
						if (dot(newNorm, normalize(p - ro)) < 0.0)
							hit = true;
						break;
					}

					dO += dS; // Add distance to distance from origin
					oldDist = dS;

					if (dO > MAX_DIST)
						break;
				}

				if (hit) {
					vec3 newColor = texture(pass, newCoords).rgb;
					combined += newColor * reflectionMult;
					samples++;
				}
			}
			if (samples != 0) {
				image.rgb = combined / samples;
				image.a = samples / SAMPLES;
			}/* else {
				vec3 samplePos = (position - cameraPosition) / voxelSize + 0.5;

				if (!(samplePos.x < 0.0 || samplePos.x > 1.0 || samplePos.y < 0.0 ||
					samplePos.y > 1.0 || samplePos.z < 0.0 || samplePos.z > 1.0)) {

					vec4 gi = vec4(vec3(0.0), 1.0);
					gi = computeGI(textureCoords, position, normalize(reflect(camToWorldNorm, N)), gDepth, projectionMatrix,
									inverseProjectionMatrix, inverseViewMatrix, roughness);
					if (gi.a > 0.0) {
						image.rgb = gi.rgb;
						image.a = 1;
					}
				}
			}*/
		}
	}
	out_Color = image;
}