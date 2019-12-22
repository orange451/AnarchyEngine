/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

in vec2 textureCoords;

out vec4 out_Color;

uniform sampler2D lensFlare;
uniform sampler2D lensDirt;
uniform sampler2D lensStar;
uniform sampler2D image;

uniform int useLensFlares;

const float mult = 2.0;

void main() {
	vec3 color = texture(image, textureCoords).rgb;
	if (useLensFlares == 1) {
		vec3 lensMod = texture(lensDirt, textureCoords).rgb;
		lensMod += texture(lensStar, textureCoords).rgb;
		vec3 lensFlare = texture(lensFlare, textureCoords).rgb * (lensMod * mult);
		color += lensFlare;
	}
	out_Color.rgb = color;
	out_Color.a = 0.0;
}