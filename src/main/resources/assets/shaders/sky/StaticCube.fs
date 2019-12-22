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

out vec4 out_Color;

uniform samplerCube environmentMap;
uniform float power;
uniform float brightness;
uniform vec3 ambient;

void main() {
	vec3 color = texture(environmentMap, pass_textureCoords).rgb;

	color = max(color, 0.0);
	color = pow(color, vec3(power));
	color *= brightness;

	out_Color = vec4(color * ambient, 0.0);
}