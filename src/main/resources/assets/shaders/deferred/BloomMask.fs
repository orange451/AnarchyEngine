/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec4 out_Color;

uniform sampler2D image;

void main() {
	vec3 color = texture(image, textureCoords).rgb;
	out_Color.rgb = max(color * 0.1, 0.0);
	out_Color.a = 0.0;
}