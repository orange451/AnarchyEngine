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

uniform vec3 cameraPosition;
uniform vec3 lightPosition;
uniform sampler2D gDiffuse;
uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform sampler2D gPBR; // R = roughness, G = metallic
uniform sampler2D gMask;
uniform sampler2D gDepth;
uniform sampler2D volumetric;
uniform samplerCube irradianceCube;
uniform samplerCube environmentCube;
uniform sampler2D brdfLUT;
uniform int useAmbientOcclusion;
uniform int useShadows;
uniform int useReflections;
uniform vec2 resolution;
uniform mat4 projectionLightMatrix[4];
uniform mat4 viewLightMatrix;
uniform mat4 biasMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2DShadow shadowMap[4];

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlick

#include function fresnelSchlickRoughness

#include function computeAmbientOcclusionV2

#include function computeShadow

// Linear depth
// float zndc = texture(gDepth, textureCoords).r;
// float A = projectionMatrix[2][2];
// float B = projectionMatrix[3][2];
// float zeye = B / (A + zndc);

#define MAX_STEPS 100
#define MAX_DIST 0.5
#define SURF_DIST 0.01

float computeContactShadows(vec3 pos, vec3 N, vec3 L, float imageDepth) {
	if (dot(N, L) < 0.0)
		return 0.0;
	float shadow = 1.0;

	vec3 newPos;
	vec4 newScreen;
	vec2 newCoords;

	float depth, newDepth;

	float tmpDepth;

	float dO = 0.0;
	float odS;

	vec3 rd = L;
	vec3 ro = pos + rd * SURF_DIST * 1.0;
	int i = 0;
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
		float dS = min(length(newPos - p), 0.01);

		if (length(newPos - p) > 0.3) {
			shadow = 1.0;
			break;
		}

		float diff = newDepth - depth;

		if (diff > 0.0) {
			shadow = 0.0;
			break;
		}

		dO += dS; // Add distance to distance from origin

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

void main() {
	vec4 mask = texture(gMask, textureCoords);
	vec4 image = texture(gDiffuse, textureCoords);
	if (mask.a != 1) {
		vec2 pbr = texture(gPBR, textureCoords).rg;
		float depth = texture(gDepth, textureCoords).r;
		vec3 position =
			positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);
		vec3 normal = texture(gNormal, textureCoords).rgb;
		float roughness = pbr.r;
		float metallic = pbr.g;

		vec3 N = normalize(normal);
		vec3 V = normalize(cameraPosition - position);
		vec3 R = reflect(-V, N);
		vec3 L = normalize(lightPosition);
		vec3 H = normalize(V + L);

		vec3 F0 = vec3(0.04);
		F0 = mix(F0, image.rgb, metallic);

		vec3 Lo = vec3(0.0);
		vec3 radiance = vec3(1.0);

		float NDF = DistributionGGX(N, H, roughness);
		float G = GeometrySmith(N, V, L, roughness);
		vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

		vec3 nominator = NDF * G * F;
		float denominator = max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001;
		vec3 brdf = nominator / denominator;

		vec3 kS = F;
		vec3 kD = 1.0 - kS;
		kD *= 1.0 - metallic;

		float NdotL = max(dot(N, L), 0.0) *
					  computeShadow(position) * computeContactShadows(position, N, L, depth);
		Lo += (kD * image.rgb / PI + brdf) * radiance * NdotL;

		F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);

		kS = F;
		kD = 1.0 - kS;
		kD *= 1.0 - metallic;

		vec3 irradiance = texture(irradianceCube, N).rgb;
		vec3 diffuse = irradiance * image.rgb;

		vec3 ambient = kD * diffuse;

		if (useReflections != 1) { // Using SSR
			vec3 prefilteredColor =
				textureLod(environmentCube, R, roughness * MAX_REFLECTION_LOD).rgb;
			vec2 envBRDF = texture(brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
			vec3 specular = prefilteredColor * (F * envBRDF.x + envBRDF.y);
			ambient += specular;
			if (useAmbientOcclusion == 1) {
				float ao = computeAmbientOcclusion(textureCoords, position, N, gDepth,
												   inverseProjectionMatrix, inverseViewMatrix);
				ambient *= ao;
			}
		}

		vec3 emissive = texture(gMask, textureCoords).rgb;
		vec3 color = ambient + emissive + Lo;
		image.rgb = color;

		//image.rgb = vec3(computeContactShadows(position, N, L, depth));
	}
	vec4 vol = texture(volumetric, textureCoords);
	image.rgb += vol.rgb;
	out_Color = image;
}