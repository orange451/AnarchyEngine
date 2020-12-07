/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

#include struct Material

#include struct PointLight

#include struct DirectionalLight

flat in int pass_axis; // indicate which axis the projection uses
flat in vec4 pass_aabb;

layout(location = 0) out vec4 gl_FragColor;
layout(pixel_center_integer) in vec4 gl_FragCoord;

in vec3 pass_pos;

in vec3 pass_position_fs;
in vec2 pass_textureCoords_fs;
in mat3 TBN_fs;

layout(rgba16f, binding = 0) restrict writeonly uniform image3D voxelImage;
uniform int resolution;

uniform Material material;
uniform vec3 cameraPosition;
uniform bool useShadows;
uniform mat4 biasMatrix;

uniform PointLight pointLights[8];
uniform int totalPointLights;

uniform DirectionalLight directionalLights[8];
uniform int totalDirectionalLights;

#include variable pi

#include function DistributionGGX

#include function GeometrySchlickGGX

#include function GeometrySmith

#include function fresnelSchlickRoughness

#include function fresnelSchlick

#include function computeShadowV2
/*
#include function calcPointLight
#include function calcDirectionalLight
*/

vec3 calcDirectionalLight(DirectionalLight light, vec3 position, vec3 diffuse, vec3 N, vec3 V,
						  vec3 F0, float roughness, float metallic) {
	if (!light.visible)
		return vec3(0.0);
	vec3 L = normalize(light.direction);
	vec3 H = normalize(V + L);
	float NdotL = max(dot(N, L), 0.0);

	vec3 radiance = light.color * light.intensity;

	vec3 F = fresnelSchlick(1.0, F0);

	vec3 nominator = F;
	float denominator = NdotL + 0.001;
	vec3 brdf = nominator / denominator;

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - metallic;

	if (light.shadows)
		NdotL *= computeShadowV2(position, light);
	return (kD * diffuse / PI) * radiance * NdotL;
}

#include function toneMap

#include variable GLOBAL

void main() {

	/*if (pass_pos.x < pass_aabb.x || pass_pos.y < pass_aabb.y || pass_pos.x > pass_aabb.z ||
		pass_pos.y > pass_aabb.w)
		discard;*/

	vec3 diffuseF = texture(material.diffuseTex, pass_textureCoords_fs).rgb;
	float roughness = texture(material.roughnessTex, pass_textureCoords_fs).r;
	float metallic = texture(material.metallicTex, pass_textureCoords_fs).r;

	diffuseF *= material.diffuse;
	roughness *= material.roughness;
	metallic *= material.metallic;

	vec3 normal = texture(material.normalTex, pass_textureCoords_fs).rgb;
	normal = normalize(normal * 2.0 - 1.0);
	normal = normalize(TBN_fs * normal);

	vec3 position = pass_position_fs.xyz;

	vec3 N = normalize(normal);
	vec3 V = normalize(cameraPosition - position);
	vec3 R = reflect(-V, N);

	vec3 F0 = vec3(0.04);
	F0 = mix(F0, diffuseF.rgb, metallic);
	vec3 Lo = vec3(0.0);

	for (int i = 0; i < totalDirectionalLights; i++) {
		Lo += calcDirectionalLight(directionalLights[i], position, diffuseF.rgb, N, V, F0,
								   roughness, metallic);
	}

	//vec3 basicNormal = normalize(TBN_fs * vec3(0,0,1));
	vec4 data = vec4(Lo + material.emissive, 1.0);

	ivec3 temp = ivec3(gl_FragCoord.x, gl_FragCoord.y, resolution * gl_FragCoord.z);
	ivec3 texcoord;
	if (pass_axis == 1) {
		texcoord.x = resolution - temp.z;
		texcoord.z = temp.x;
		texcoord.y = temp.y;
	} else if (pass_axis == 2) {
		texcoord.z = temp.y;
		texcoord.y = resolution - temp.z;
		texcoord.x = temp.x;
	} else
		texcoord = temp;

	imageStore(voxelImage, texcoord, data);
}
