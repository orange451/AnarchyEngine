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

#variable MASK
#define PBR_OBJECT 0x0
#define PBR_BACKGROUND 0x1
#define PBR_BACKGROUND_DYNAMIC 0x2
#end

#struct Light
struct Light {
	vec3 position;
	vec3 color;
	float radius;
	float intensity;
};
#end

#function DistributionGGX
float DistributionGGX(vec3 N, vec3 H, float roughness) {
	float a = roughness * roughness;
	float a2 = a * a;
	float NdotH = max(dot(N, H), 0.0);
	float NdotH2 = NdotH * NdotH;

	float nom = a2;
	float denom = (NdotH2 * (a2 - 1.0) + 1.0);
	denom = PI * denom * denom;

	return nom / denom;
}
#end

#function GeometrySchlickGGX
float GeometrySchlickGGX(float NdotV, float roughness) {
	float r = (roughness + 1.0);
	float k = (r * r) / 8.0;

	float nom = NdotV;
	float denom = NdotV * (1.0 - k) + k;

	return nom / denom;
}
#end

#function GeometrySmith
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
	float NdotV = max(dot(N, V), 0.0);
	float NdotL = max(dot(N, L), 0.0);
	float ggx2 = GeometrySchlickGGX(NdotV, roughness);
	float ggx1 = GeometrySchlickGGX(NdotL, roughness);

	return ggx1 * ggx2;
}
#end

#function fresnelSchlickRoughness
vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
	return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(1.0 - cosTheta, 5.0);
}
#end

#function fresnelSchlick
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
	return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}
#end

#function computeAmbientOcclusion
const float distanceThreshold = 1;
const int sample_count = 16;
const vec2 poisson16[] = vec2[](
	vec2(-0.94201624, -0.39906216), vec2(0.94558609, -0.76890725), vec2(-0.094184101, -0.92938870),
	vec2(0.34495938, 0.29387760), vec2(-0.91588581, 0.45771432), vec2(-0.81544232, -0.87912464),
	vec2(-0.38277543, 0.27676845), vec2(0.97484398, 0.75648379), vec2(0.44323325, -0.97511554),
	vec2(0.53742981, -0.47373420), vec2(-0.26496911, -0.41893023), vec2(0.79197514, 0.19090188),
	vec2(-0.24188840, 0.99706507), vec2(-0.81409955, 0.91437590), vec2(0.19984126, 0.78641367),
	vec2(0.14383161, -0.14100790));

float computeAmbientOcclusion(vec3 position, vec3 normal) {
	if (useAmbientOcclusion == 1) {
		float ambientOcclusion = 0;
		vec2 filterRadius = vec2(10 / resolution.x, 10 / resolution.y);
		for (int i = 0; i < sample_count; ++i) {
			vec2 sampleTexCoord = textureCoords + (poisson16[i] * (filterRadius));
			float sampleDepth = texture(gDepth, sampleTexCoord).r;
			vec3 samplePos = texture(gPosition, sampleTexCoord).rgb;
			vec3 sampleDir = normalize(samplePos - position);
			float NdotS = max(dot(normal, sampleDir), 0);
			float VPdistSP = distance(position, samplePos);
			float a = 1.0 - smoothstep(distanceThreshold, distanceThreshold * 2, VPdistSP);
			float b = NdotS;
			ambientOcclusion += (a * b) * 1.3;
		}
		return -(ambientOcclusion / sample_count) + 1;
	} else
		return 1.0;
}
#end

#function computeShadow

vec4 ShadowCoord[4];

vec2 multTex[4];

float lookup(vec2 offset) {
	if (ShadowCoord[3].x > 0 && ShadowCoord[3].x < 1 && ShadowCoord[3].y > 0 &&
		ShadowCoord[3].y < 1) {
		if (ShadowCoord[2].x > 0 && ShadowCoord[2].x < 1 && ShadowCoord[2].y > 0 &&
			ShadowCoord[2].y < 1) {
			if (ShadowCoord[1].x > 0 && ShadowCoord[1].x < 1 && ShadowCoord[1].y > 0 &&
				ShadowCoord[1].y < 1) {
				if (ShadowCoord[0].x > 0 && ShadowCoord[0].x < 1 && ShadowCoord[0].y > 0 &&
					ShadowCoord[0].y < 1) {
					offset *= multTex[0];
					return texture(shadowMap[0], ShadowCoord[0].xyz + vec3(offset.x, offset.y, 0));
				}
				offset *= multTex[1];
				return texture(shadowMap[1], ShadowCoord[1].xyz + vec3(offset.x, offset.y, 0));
			}
			offset *= multTex[2];
			return texture(shadowMap[2], ShadowCoord[2].xyz + vec3(offset.x, offset.y, 0));
		}
		offset *= multTex[3];
		return texture(shadowMap[3], ShadowCoord[3].xyz + vec3(offset.x, offset.y, 0));
	}
	return 1.0;
}

float computeShadow(vec3 position) {
	if (useShadows == 1) {
		float shadow = 0.0;
		vec4 posLight = viewLightMatrix * vec4(position, 1.0);
		ShadowCoord[0] = biasMatrix * (projectionLightMatrix[0] * posLight);
		multTex[0] = 1.0 / textureSize(shadowMap[0], 0);
		ShadowCoord[1] = biasMatrix * (projectionLightMatrix[1] * posLight);
		multTex[1] = 1.0 / textureSize(shadowMap[1], 0);
		ShadowCoord[2] = biasMatrix * (projectionLightMatrix[2] * posLight);
		multTex[2] = 1.0 / textureSize(shadowMap[2], 0);
		ShadowCoord[3] = biasMatrix * (projectionLightMatrix[3] * posLight);
		multTex[3] = 1.0 / textureSize(shadowMap[3], 0);
		for (int x = -1; x <= 1; ++x) {
			for (int y = -1; y <= 1; ++y) {
				shadow += lookup(vec2(x, y));
			}
		}
		return shadow / 9.0;
	} else
		return 1.0;
}
#end

#function getDepth
float getDepth(mat4 proj, sampler2D depth, vec2 texcoord) {
	float zndc = texture(depth, texcoord).r;
#ifdef OneToOneDepth
	zndc = zndc * 2.0 - 1.0;
#endif
	float A = proj[2][2];
	float B = proj[3][2];
	return B / (A + zndc);
}
#end

#function positionFromDepth
vec3 positionFromDepth(vec2 texCoords, float depth, mat4 invProjection, mat4 invView) {
#ifdef OneToOneDepth
	vec4 currentPosition = vec4(texCoords * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
#else
	vec4 currentPosition = vec4(texCoords * 2.0 - 1.0, depth, 1.0);
#endif
	vec4 position = invProjection * currentPosition;
	position = invView * position;
	position.xyz /= position.w;
	return position.xyz;
}
#end

#function computeAmbientOcclusionV2
const float distanceThreshold = 1;
const int sample_count = 16;
const vec2 poisson16[] = vec2[](
	vec2(-0.94201624, -0.39906216), vec2(0.94558609, -0.76890725), vec2(-0.094184101, -0.92938870),
	vec2(0.34495938, 0.29387760), vec2(-0.91588581, 0.45771432), vec2(-0.81544232, -0.87912464),
	vec2(-0.38277543, 0.27676845), vec2(0.97484398, 0.75648379), vec2(0.44323325, -0.97511554),
	vec2(0.53742981, -0.47373420), vec2(-0.26496911, -0.41893023), vec2(0.79197514, 0.19090188),
	vec2(-0.24188840, 0.99706507), vec2(-0.81409955, 0.91437590), vec2(0.19984126, 0.78641367),
	vec2(0.14383161, -0.14100790));

float computeAmbientOcclusion(vec2 texCoords, vec3 position, vec3 normal, sampler2D gDepth,
							  mat4 invProjection, mat4 invView) {
	float ambientOcclusion = 0;
	vec2 filterRadius = vec2(10) / resolution;
	for (int i = 0; i < sample_count; ++i) {
		vec2 sampleTexCoord = texCoords + (poisson16[i] * filterRadius);
		float depth = texture(gDepth, sampleTexCoord).r;
		vec3 samplePos = positionFromDepth(sampleTexCoord, depth, invProjection, invView);
		vec3 sampleDir = normalize(samplePos - position);
		float NdotS = max(dot(normal, sampleDir), 0.0);
		float VPdistSP = distance(position, samplePos);
		float a = 1.0 - smoothstep(distanceThreshold, distanceThreshold * 2, VPdistSP);
		float b = NdotS;
		ambientOcclusion += max(a * b, 0.0) * 1.5;
	}
	return -(ambientOcclusion / sample_count) + 1.0;
}
#end