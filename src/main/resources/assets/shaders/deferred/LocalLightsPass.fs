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

#include struct Light

in vec2 textureCoords;

out vec4 out_Color;

uniform vec3 cameraPosition;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat4 inverseViewMatrix;

uniform Light lights[128];
uniform int totalLights;
uniform mat4 biasMatrix;

uniform int useShadows;

uniform sampler2D gDiffuse;
uniform sampler2D gNormal;
uniform sampler2D gMask;
uniform sampler2D gPBR; // R = roughness, B = metallic
uniform sampler2D gDepth;
uniform sampler2D image;

#include variable pi

#include function positionFromDepth

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlick

vec3 calcLight(Light light, vec3 position, vec3 diffuse, vec3 L, vec3 N, vec3 V, vec3 F0,
			   float roughness, float metallic) {
	vec3 H = normalize(V + L);
	float distance = length(light.position - position);
	float attenuation = max(1.0 - distance / light.radius, 0.0) / distance;
	vec3 radiance = light.color * attenuation * light.intensity;

	float NDF = DistributionGGX(N, H, roughness);
	float G = GeometrySmith(N, V, L, roughness);
	vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

	vec3 nominator = NDF * G * F;
	float denominator = max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001;
	vec3 brdf = nominator / denominator;

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - metallic;

	float NdotL = max(dot(N, L), 0.0);
	return (kD * diffuse / PI + brdf) * radiance * NdotL;
}

void main() {
	vec4 composite = texture(image, textureCoords);
	vec4 mask = texture(gMask, textureCoords);
	if (mask.a != 1) {
		vec4 diffuse = texture(gDiffuse, textureCoords);
		vec2 pbr = texture(gPBR, textureCoords).rg;
		float depth = texture(gDepth, textureCoords).r;
		vec3 position =
			positionFromDepth(textureCoords, depth, inverseProjectionMatrix, inverseViewMatrix);
		vec3 normal = texture(gNormal, textureCoords).rgb;

		vec3 N = normalize(normal);
		vec3 V = normalize(cameraPosition - position);

		float roughness = pbr.r;
		float metallic = pbr.g;

		vec3 F0 = vec3(0.04);
		F0 = mix(F0, diffuse.rgb, metallic);

		vec3 Lo = vec3(0.0);
		for (int i = 0; i < totalLights; i++) {
			vec3 L = normalize(lights[i].position - position);
			// switch (lights[i].type) {
			//	case 0:
			Lo += calcLight(lights[i], position, diffuse.rgb, L, N, V, F0, roughness, metallic);
			//	break;
			//case 1:
			//	float theta = dot(L, normalize(-lights[i].direction));
			//	float epsilon = lights[i].inRadius - lights[i].radius;
			//	float intensity = clamp((theta - lights[i].radius) / epsilon, 0.0, 1.0);
			//	if (intensity > 0.0) {
			//		float shadow = 1.0;
			//		if (lights[i].useShadows == 1 && useShadows == 1) {
			//			vec4 posLight = lights[i].viewMatrix * vec4(position, 1.0);
			//			vec4 shadowCoord =
			//				biasMatrix * (lights[i].projectionMatrix * posLight);
			//			shadow = texture(lights[i].shadowMap, (shadowCoord.xyz / shadowCoord.w), 0);
			//		}
			//		Lo += calcLight(lights[i], position, diffuse.rgb, L, N, V, F0, roughness,
			//						metallic) *
			//			  intensity * shadow;
			//	}
			//	break;
			//}
		}
		composite.rgb += Lo;
	}
	out_Color = composite;
}
