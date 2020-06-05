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
uniform sampler3D voxelImage;
uniform float time;

uniform bool useAmbientOcclusion;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function getDepth

//#include function computeAmbientOcclusionV2

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
float random(vec2 pos, float time) {
	uint mantissaMask = 0x007FFFFFu;
	uint one = 0x3F800000u;
	uvec3 u = floatBitsToUint(vec3(pos, time));
	uint h = hash3(u.x, u.y, u.z);
	return uintBitsToFloat((h & mantissaMask) | one) - 1.0;
}

vec3 randomCosineWeightedHemispherePoint(vec3 rand, vec3 n) {
	float r = rand.x * 0.5 + 0.5;	   // [-1..1) -> [0..1)
	float angle = (rand.y + 1.0) * PI; // [-1..1] -> [0..2*PI)
	float sr = sqrt(r);
	vec2 p = vec2(sr * cos(angle), sr * sin(angle));

	vec3 ph = vec3(p.xy, sqrt(1.0 - p * p));

	vec3 tangent = normalize(rand);
	vec3 bitangent = cross(tangent, n);
	tangent = cross(bitangent, n);

	return tangent * ph.x + bitangent * ph.y + n * ph.z;
}

#define SAMPLES_AMBIENT 4
#define MAX_STEPS_AMBIENT 100
#define MAX_DIST_AMBIENT 20.0
#define SURF_DIST_AMBIENT 1.0
#define SAMPLE_STEP 0.25
#define AREA_SIZE 40
#define VOXEL_RES 256

const float AMBIENT_SIZE = (AREA_SIZE * 2);
const float OFFSET = (AMBIENT_SIZE / VOXEL_RES) * 0.0;

vec4 computeAmbient(vec2 texCoords, vec3 position, vec3 normal, sampler2D gDepth, mat4 projection,
					mat4 invProjection, mat4 invView) {
	vec4 ambientCombined = vec4(0.0);
	int j = 0;
	for (j = 0; j < SAMPLES_AMBIENT; j++) {
		vec4 ambient = vec4(vec3(0.0), 1.0);
		float dO = 0.0;

		float rand1 = random(textureCoords + position.xy, time + float(j));
		float rand2 =
			random(textureCoords + vec2(641.51224, 423.178) + position.zy, time + float(j));
		float rand3 =
			random(textureCoords - vec2(147.16414, 363.941) + position.xz, time - float(j));
		vec3 rand = vec3(rand1, rand2, rand3);

		vec3 rd = randomCosineWeightedHemispherePoint(rand, normal);
		vec3 ro = position + rd * SURF_DIST_AMBIENT;
		int i = 0;
		for (i = 0; i < MAX_STEPS_AMBIENT; i++) {

			vec3 p = ro + rd * dO;

			dO += SAMPLE_STEP;
			vec4 samplePoint = texture(
				voxelImage, vec3((p.xyz - cameraPosition + vec3(OFFSET)) / AMBIENT_SIZE + 0.5));

			if (samplePoint.a > 0.0) {
				ambient.rgb += samplePoint.rgb;
				ambient.a = 0.0;
				break;
			}

			if (dO > MAX_DIST_AMBIENT) {
				ambient = vec4(vec3(0.0), 1.0);
				break;
			}
		}
		if (i == MAX_STEPS_AMBIENT) {
			ambient = vec4(vec3(0.0), 1.0);
		}
		ambientCombined += ambient;
	}
	return ambientCombined / j;
}

#include variable MASK

void main() {
	vec4 mask = texture(gMask, textureCoords);
	vec4 image = vec4(vec3(0.0), 1.0);
	if (useAmbientOcclusion) {

		float depth = texture(gDepth, textureCoords).r;
		vec3 position =
			positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);
		vec3 normal = texture(gNormal, textureCoords).rgb;
		vec3 N = normalize(normal);

		/*vec3 rd = normalize(position - cameraPosition);

		vec3 cameraToWorld = position - cameraPosition;
		float cameraToWorldDist = length(cameraToWorld);

		vec3 rayTrace = cameraPosition;
		float rayDist, incr = 0.025;
		float rayDistRad = length(position - cameraPosition);
		do {
			rayTrace += rd * incr;
			incr *= 1.010;

			rayDist = length(rayTrace - cameraPosition);
			vec4 pointSample = texture(voxelImage, vec3((rayTrace.xyz - cameraPosition + vec3(OFFSET)) / AMBIENT_SIZE + 0.5));
			if (pointSample.a > 0.0) {
				image.rgb += pointSample.rgb;
				break;
			}
		} while (rayDist < 200);*/

		vec4 ambient = computeAmbient(textureCoords, position, N, gDepth, projectionMatrix,
									  inverseProjectionMatrix, inverseViewMatrix);
		image = vec4(ambient);
	}
	out_Color = image;
}