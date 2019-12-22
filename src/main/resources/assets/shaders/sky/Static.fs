/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

in vec3 pass_textureCoords;
in vec3 pass_position;
in vec4 clipSpace;
in vec4 clipSpacePrev;

out vec4[5] out_Color;

uniform samplerCube environmentMap;
uniform float power;
uniform float brightness;
uniform vec3 ambient;

#include variable MASK

void main() {
	vec3 color = texture(environmentMap, pass_textureCoords).rgb;

	color = max(color, 0.0);
	color = pow(color, vec3(power));
	color *= brightness;

	vec3 ndcPos = (clipSpace / clipSpace.w).xyz;
	vec3 ndcPosPrev = (clipSpacePrev / clipSpacePrev.w).xyz;

	out_Color[0] = vec4(color * ambient, 0.0);
	out_Color[1] = vec4((ndcPosPrev - ndcPos).xy, 0.0, 0.0);
	out_Color[2] = vec4(0.0);
	out_Color[3] = vec4(0.0);
	out_Color[4] = vec4(0.0, 0.0, 0.0, PBR_BACKGROUND);
}