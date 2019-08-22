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

#include struct Material

in vec2 pass_textureCoords;
in vec4 pass_position;
in mat3 TBN;

out vec4 out_Color;

uniform int colorCorrect;
uniform Material material;
uniform vec3 cameraPosition;
uniform vec3 lightPosition;
uniform samplerCube irradianceMap;
uniform samplerCube preFilterEnv;
uniform sampler2D brdfLUT;
uniform int useShadows;
uniform mat4 projectionLightMatrix[4];
uniform mat4 viewLightMatrix;
uniform mat4 biasMatrix;
uniform sampler2DShadow shadowMap[4];

#include variable pi

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlickRoughness

#include function fresnelSchlick

#include function computeShadow

#define exposure 1.0

#include variable GLOBAL

void main() {

	vec4 diffuseF = texture(material.diffuseTex, pass_textureCoords);
	float roughness = texture(material.roughnessTex, pass_textureCoords).r;
	float metallic = texture(material.metallicTex, pass_textureCoords).r;

	diffuseF *= material.diffuse;
	roughness *= material.roughness;
	metallic *= material.metallic;
	
	if (diffuseF.a == 0.0)
		discard;

	vec3 normal = texture(material.normalTex, pass_textureCoords).rgb;
	normal = normalize(normal * 2.0 - 1.0);
	normal = normalize(TBN * normal);

	vec3 position = pass_position.xyz;

	vec3 N = normalize(normal);
	vec3 V = normalize(cameraPosition - position);
	vec3 R = reflect(-V, N);
	vec3 L = normalize(lightPosition);
	vec3 H = normalize(V + L);

	vec3 F0 = vec3(0.04);
	F0 = mix(F0, diffuseF.rgb, metallic);

	vec3 Lo = vec3(0.0);
	vec3 radiance = vec3(1.0);

	float NDF = DistributionGGX(N, H, roughness);
	float G = GeometrySmith(N, V, L, roughness);
	vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

	vec3 nominator = NDF * G * F;
	float denominator = max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001;
	vec3 brdf = nominator / denominator;

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - metallic;

	float NdotL = max(dot(N, L), 0.0) * computeShadow(position);
	Lo += (kD * diffuseF.rgb / PI + brdf) * radiance * NdotL;

	F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);

	kS = F;
	kD = 1.0 - kS;
	kD *= 1.0 - metallic;

	vec3 irradiance = texture(irradianceMap, N).rgb;
	vec3 diffuse = irradiance * diffuseF.rgb;

	vec3 prefilteredColor = textureLod(preFilterEnv, R, roughness * MAX_REFLECTION_LOD).rgb;
	vec2 envBRDF = texture(brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
	vec3 specular = prefilteredColor * (F * envBRDF.x + envBRDF.y);

	vec3 emissive = material.emissive.rgb * diffuseF.rgb;

	vec3 ambient = kD * diffuse + specular;
	vec3 color = ambient + emissive + Lo;
	if (colorCorrect == 1) {
		vec3 final = vec3(1.0) - exp(-color * exposure);
		final = pow(final, vec3(1.0 / GAMMA));
		out_Color = vec4(final, diffuseF.a);
	} else
		out_Color = vec4(color, diffuseF.a);
}
