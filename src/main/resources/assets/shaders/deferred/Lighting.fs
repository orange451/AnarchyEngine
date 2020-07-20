/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec4[2] out_Color;

uniform vec2 resolution;
uniform vec3 cameraPosition;
uniform sampler2D gDiffuse;
uniform sampler2D gNormal;
uniform sampler2D gPBR; // R = roughness, G = metallic
uniform sampler2D gMask;
uniform sampler2D gDepth;
// uniform sampler2D volumetric;
uniform samplerCube irradianceCube;
uniform samplerCube environmentCube;
uniform sampler2D brdfLUT;
uniform mat4 biasMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2D directionalLightData;
uniform sampler2D pointLightData;
uniform sampler2D spotLightData;
uniform sampler2D areaLightData;
uniform sampler2D ambientOcclusion;

uniform bool useAmbientOcclusion;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlick

#include function fresnelSchlickRoughness

#include function getDepth

#include variable MASK

// Linear depth
// float zndc = texture(gDepth, textureCoords).r;
// float A = projectionMatrix[2][2];
// float B = projectionMatrix[3][2];
// float zeye = B / (A + zndc);

#define MAX_STEPS 100
#define MAX_DIST 1.0
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
	vec3 image = texture(gDiffuse, textureCoords).rgb;
	vec4 lightData = vec4(0.0);
	vec4 auxData = vec4(0.0);

	lightData += texture(spotLightData, textureCoords);

	if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
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
		float ndotv = max(dot(N, V), 0.0);

		vec3 F0 = vec3(0.04);
		F0 = mix(F0, image.rgb, metallic);
		vec3 Lo = vec3(0.0);

		Lo += texture(directionalLightData, textureCoords).rgb;
		Lo += texture(pointLightData, textureCoords).rgb;
		Lo += lightData.rgb;
		Lo += texture(areaLightData, textureCoords).rgb;

		vec3 F = fresnelSchlickRoughness(ndotv, F0, roughness);

		vec3 kS = F;
		vec3 kD = vec3(1.0) - kS;
		kD *= 1.0 - metallic;

		vec3 irradiance = texture(irradianceCube, N).rgb;
		vec3 diffuse = irradiance * image.rgb;

		vec3 ambient = kD * diffuse;

		vec3 prefilteredColor = textureLod(environmentCube, R, roughness * MAX_REFLECTION_LOD).rgb;
		vec2 envBRDF = texture(brdfLUT, vec2(ndotv, roughness)).rg;
		vec3 specular = prefilteredColor * (F * envBRDF.x + envBRDF.y);

		//ambient += specular;

		auxData.a = 1.0;	 // Initial value 1.0

		if (useAmbientOcclusion) {
			vec4 ssAmbient = texture(ambientOcclusion, textureCoords);
			float ao = ssAmbient.a;
			auxData.a = ao; // Store AO for use in SSR pass
			ambient *= ao;	// Apply AO, operation will be reversed in SSR pass
			ambient += kD * ssAmbient.rgb * image.rgb;
		}
		vec3 light = ambient + Lo;

		auxData.rgb = ambient; // Store light for use in SSR pass

		vec3 emissive = texture(gMask, textureCoords).rgb;
		vec3 color = light + emissive;
		image.rgb = color;
	}
	out_Color[0].rgb = image.rgb;
	out_Color[0].a = 0.0;
	out_Color[1] = auxData;
}