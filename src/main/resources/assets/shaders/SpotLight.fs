/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

#include struct SpotLight

in vec3 pass_position;

out vec4 out_Color;

uniform vec2 texel;
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

uniform SpotLight light;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlick

#include function fresnelSchlickRoughness

#include function calcSpotLight

#include variable MASK

#define MAX_STEPS 200
#define MAX_DIST 50000.
#define SURF_DIST .01

float GetDist(vec3 p) {
	float sphereDist = length(p - light.position) - light.radius;
	return sphereDist;
}

float RayMarch(vec3 ro, vec3 rd) {
	float dO = 0.0;
	for (int i = 0; i < MAX_STEPS; i++) {
		vec3 p = ro + rd * dO;
		float dS = GetDist(p);
		dO += dS;
		if (dO > MAX_DIST || dS < SURF_DIST)
			break;
	}
	return dO;
}

void main() {
	vec2 textureCoords = gl_FragCoord.xy * texel;

	vec4 mask = texture(gMask, textureCoords);
	vec3 result = vec3(0.0);
	float depth = texture(gDepth, textureCoords).r;
	vec3 position =
		positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);
	if (MASK_COMPARE(mask.a, PBR_OBJECT)) {
		vec4 image = texture(gDiffuse, textureCoords);
		vec2 pbr = texture(gPBR, textureCoords).rg;
		vec3 normal = texture(gNormal, textureCoords).rgb;
		float roughness = pbr.r;
		float metallic = pbr.g;

		vec3 N = normalize(normal);
		vec3 V = normalize(cameraPosition - position);

		vec3 F0 = vec3(0.04);
		F0 = mix(F0, image.rgb, metallic);

		result = calcSpotLight(light, position, image.rgb, N, V, F0, roughness, metallic);
	}

	/*vec3 rd = normalize(pass_position - cameraPosition);
	vec3 ro = cameraPosition;

	float d = RayMarch(ro, rd);
	vec3 p = ro + rd * d;

	vec3 cameraToWorld = position.xyz - cameraPosition;
	float cameraToWorldDist = length(cameraToWorld);

	vec3 rayTrace;
	if (d > 0.0)
		rayTrace = p;
	else
		rayTrace = cameraPosition;
	float rayDist, incr = 0.1;
	float rayDistRad = length(pass_position - cameraPosition);
	vec3 rays;
	vec3 randSample, finalTrace;
	do {
		rayTrace += rd * incr;
		incr *= 1.05;

		rayDist = length(rayTrace - cameraPosition);
		if (rayDist > cameraToWorldDist)
			break;

		vec3 L = normalize(light.position - rayTrace);
		float distance = length(light.position - rayTrace);
		float attenuation = max(1.0 - distance / light.radius, 0.0) / distance;
		float theta = dot(L, normalize(-light.direction));
		float epsilon = light.innerFOV - light.outerFOV;
		float intensity = clamp((theta - light.outerFOV) / epsilon, 0.0, 1.0);

		vec4 posLight = light.viewMatrix * vec4(rayTrace, 1.0);
		vec4 shadowCoord = biasMatrix * (light.projectionMatrix * posLight);
		vec2 multTex = 1.0 / textureSize(light.shadowMap, 0).xy;
		float shadow = 0.0;
		for (int x = -2; x <= 2; ++x) {
			for (int y = -2; y <= 2; ++y) {
				vec2 offset = vec2(x, y) * multTex;
				vec3 temp = shadowCoord.xyz + vec3(offset * shadowCoord.z, 0);
				shadow += texture(light.shadowMap, (temp / shadowCoord.w), 0);
			}
		}
		vec3 rayColor = light.color * light.intensity * 0.002;
		rays += rayColor * intensity * attenuation * (shadow / 25.0) * incr;
		if (rayDist > rayDistRad)
			break;
	} while (rayDist < rayDistRad);*/
	out_Color.rgb = result;
	//out_Color.a = rays.r;
}