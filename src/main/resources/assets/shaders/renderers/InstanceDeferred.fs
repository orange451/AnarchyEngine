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
in vec3 pass_position;
in mat3 TBN;
in vec4 clipSpace;
in vec4 clipSpacePrev;
in vec3 pass_normal;

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
	out_Color[2] = vec4(pass_normal, 0.0);
	out_Color[3] = vec4(0.0, metallicF, 0.0, 0.0);
	out_Color[4] = vec4(material.emissive.rgb * diffuseF.rgb, PBR_OBJECT);
}
