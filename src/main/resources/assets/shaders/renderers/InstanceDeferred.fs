/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

#include struct Material

in vec2 pass_textureCoords;
in vec3 pass_position;
in mat3 TBN;
in vec4 clipSpace;
in vec4 clipSpacePrev;

out vec4[5] out_Color;

uniform Material material;

#include variable MASK

void main() {

	vec4 diffuseF = texture(material.diffuseTex, pass_textureCoords);
	float roughnessF = texture(material.roughnessTex, pass_textureCoords).r;
	float metallicF = texture(material.metallicTex, pass_textureCoords).r;

	diffuseF.rgb *= material.diffuse;
	roughnessF *= material.roughness;
	metallicF *= material.metallic;

	if (diffuseF.a <= 0.5)
		discard;

	vec3 norm = texture(material.normalTex, pass_textureCoords).rgb;
	vec3 map = vec3(norm.x, norm.y, 1.0);
	map = map * -2.0 + 1.0;
	map.z = sqrt(1.0 - dot(map.xx, map.yy));
	map.y = map.y;
	vec3 normal = normalize(TBN * map);

	vec3 ndcPos = (clipSpace / clipSpace.w).xyz;
    vec3 ndcPosPrev = (clipSpacePrev / clipSpacePrev.w).xyz;

	out_Color[0] = vec4(diffuseF.rgb, 0.0);
	out_Color[1] = vec4((ndcPosPrev - ndcPos).xy, 0.0, 0.0);
	out_Color[2] = vec4(normal, 0.0);
	out_Color[3] = vec4(roughnessF, metallicF, 0.0, 0.0);
	out_Color[4] = vec4(material.emissive.rgb * diffuseF.rgb, PBR_OBJECT);
}
