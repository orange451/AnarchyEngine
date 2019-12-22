/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

in vec2 textureCoords0;
in vec2 textureCoords1;
in float blend;

out vec4 out_Color;

uniform sampler2D particleTexture;

void main() {
	vec4 color = mix(texture(particleTexture, textureCoords0),
					texture(particleTexture, textureCoords1), blend);
	if (color.a == 0)
		discard;
	out_Color = color;
}