/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

#include struct AreaLight

in vec2 textureCoords;

out vec3 out_Color;

uniform vec3 cameraPosition;
uniform sampler2D gDiffuse;
uniform sampler2D gNormal;
uniform sampler2D gPBR; // R = roughness, G = metallic
uniform sampler2D gMask;
uniform sampler2D gDepth;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;
uniform sampler2D ltcMag;
uniform sampler2D ltcMat;
uniform mat4 transformationMatrix;
uniform vec3 points[4];

uniform AreaLight light;

#include variable GLOBAL

#include variable pi

#include function positionFromDepth

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlick

#include function fresnelSchlickRoughness

#include variable MASK

/*
Real-Time Polygonal-Light Shading with Linearly Transformed Cosines.
Eric Heitz, Jonathan Dupuy, Stephen Hill and David Neubelt.
ACM Transactions on Graphics (Proceedings of ACM SIGGRAPH 2016) 35(4), 2016.
Project page: https://eheitzresearch.wordpress.com/415-2/
*/

const float LUT_SIZE = 64.0;
const float LUT_SCALE = (LUT_SIZE - 1.0) / LUT_SIZE;
const float LUT_BIAS = 0.5 / LUT_SIZE;

vec3 integrate(vec3 v1, vec3 v2) {
	float x = dot(v1, v2);
	float y = abs(x);

	float a = 0.8543985 + (0.4965155 + 0.0145206 * y) * y;
	float b = 3.4175940 + (4.1616724 + y) * y;
	float v = a / b;

	float theta_sintheta = (x > 0.0) ? v : 0.5 * inversesqrt(max(1.0 - x * x, 1e-7)) - v;

	return cross(v1, v2) * theta_sintheta;
}

void clipQuad(inout vec3 L[5], out int n) {
	int config = 0;
	if (L[0].z > 0.0)
		config += 1;
	if (L[1].z > 0.0)
		config += 2;
	if (L[2].z > 0.0)
		config += 4;
	if (L[3].z > 0.0)
		config += 8;

	// clip
	n = 0;

	if (config == 0) {
	} else if (config == 1) {
		n = 3;
		L[1] = -L[1].z * L[0] + L[0].z * L[1];
		L[2] = -L[3].z * L[0] + L[0].z * L[3];
	} else if (config == 2) {
		n = 3;
		L[0] = -L[0].z * L[1] + L[1].z * L[0];
		L[2] = -L[2].z * L[1] + L[1].z * L[2];
	} else if (config == 3) {
		n = 4;
		L[2] = -L[2].z * L[1] + L[1].z * L[2];
		L[3] = -L[3].z * L[0] + L[0].z * L[3];
	} else if (config == 4) {
		n = 3;
		L[0] = -L[3].z * L[2] + L[2].z * L[3];
		L[1] = -L[1].z * L[2] + L[2].z * L[1];
	} else if (config == 5) {
		n = 0;
	} else if (config == 6) {
		n = 4;
		L[0] = -L[0].z * L[1] + L[1].z * L[0];
		L[3] = -L[3].z * L[2] + L[2].z * L[3];
	} else if (config == 7) {
		n = 5;
		L[4] = -L[3].z * L[0] + L[0].z * L[3];
		L[3] = -L[3].z * L[2] + L[2].z * L[3];
	} else if (config == 8) {
		n = 3;
		L[0] = -L[0].z * L[3] + L[3].z * L[0];
		L[1] = -L[2].z * L[3] + L[3].z * L[2];
		L[2] = L[3];
	} else if (config == 9) {
		n = 4;
		L[1] = -L[1].z * L[0] + L[0].z * L[1];
		L[2] = -L[2].z * L[3] + L[3].z * L[2];
	} else if (config == 10) {
		n = 0;
	} else if (config == 11) {
		n = 5;
		L[4] = L[3];
		L[3] = -L[2].z * L[3] + L[3].z * L[2];
		L[2] = -L[2].z * L[1] + L[1].z * L[2];
	} else if (config == 12) {
		n = 4;
		L[1] = -L[1].z * L[2] + L[2].z * L[1];
		L[0] = -L[0].z * L[3] + L[3].z * L[0];
	} else if (config == 13) {
		n = 5;
		L[4] = L[3];
		L[3] = L[2];
		L[2] = -L[1].z * L[2] + L[2].z * L[1];
		L[1] = -L[1].z * L[0] + L[0].z * L[1];
	} else if (config == 14) {
		n = 5;
		L[4] = -L[0].z * L[3] + L[3].z * L[0];
		L[0] = -L[0].z * L[1] + L[1].z * L[0];
	} else if (config == 15) {
		n = 4;
	}

	if (n == 3)
		L[3] = L[0];
	if (n == 4)
		L[4] = L[0];
}

vec3 ltcAreaLight(vec3 N, vec3 V, vec3 P, mat3 minv, vec3 points[4]) {
	vec3 T1, T2;
	T1 = normalize(V - N * dot(V, N));
	T2 = cross(N, T1);

	minv = minv * transpose(mat3(T1, T2, N));

	vec3 L[5];
	L[0] = minv * (points[0] - P);
	L[1] = minv * (points[1] - P);
	L[2] = minv * (points[2] - P);
	L[3] = minv * (points[3] - P);

	int n;
	clipQuad(L, n);

	if (n == 0)
		return vec3(0, 0, 0);
	L[0] = normalize(L[0]);
	L[1] = normalize(L[1]);
	L[2] = normalize(L[2]);
	L[3] = normalize(L[3]);
	L[4] = normalize(L[4]);

	vec3 vsum = vec3(0.0);

	vsum += integrate(L[0], L[1]);
	vsum += integrate(L[1], L[2]);
	vsum += integrate(L[2], L[3]);
	if (n >= 4)
		vsum += integrate(L[3], L[4]);
	if (n == 5)
		vsum += integrate(L[4], L[0]);

	vec3 fsum = inverse(minv) * vsum;

	return fsum;
}

const mat3 diffuseMatrix = mat3(1);

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

		vec3 planeDiffuseDir = ltcAreaLight(N, V, position, diffuseMatrix, points);

		float theta = acos(dot(N, V));
		vec2 uv = vec2(max(roughness, 0.020), theta / (0.5 * PI));
		uv = uv * LUT_SCALE + LUT_BIAS;

		vec4 t = texture(ltcMat, uv);
		mat3 minv = mat3(vec3(1.0, 0.0, t.y), vec3(0.0, t.z, 0.0), vec3(t.w, 0.0, t.x));

		vec3 planeSpecularDir = ltcAreaLight(N, V, position, minv, points);

		float NdotL = max(dot(N, planeDiffuseDir), 0.0);
		NdotL /= 2.0 * PI;

		vec3 L = normalize(planeSpecularDir);
		vec3 H = normalize(V + L);
		float specularNdotL = max(dot(N, L), 0.0);
		specularNdotL /= 2.0 * PI;

		vec3 radiance = vec3(light.color * light.intensity);

		float NDF = max(dot(N, planeSpecularDir), 0.0) * texture(ltcMag, uv).r / 2.0 * PI;
		float G = GeometrySmith(N, V, L, roughness) / 2.0 * PI;
		vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

		vec3 nominator = NDF * G * F;
		float denominator = max(dot(N, V), 0.0) * specularNdotL + 0.001;
		vec3 brdf = nominator / denominator;

		vec3 kS = F;
		vec3 kD = vec3(1.0) - kS;
		kD *= 1.0 - metallic;

		result = (kD * image.rgb / PI + brdf) * radiance * NdotL;
	}
	out_Color = result;
}