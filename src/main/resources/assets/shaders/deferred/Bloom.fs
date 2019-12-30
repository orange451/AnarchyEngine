/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec4 out_Color;

uniform sampler2D base;
uniform sampler2D bloom;

void main() {
	vec3 baseColor = texture(base, textureCoords).rgb;
	//baseColor += texture(bloom, textureCoords).rgb;
	out_Color.rgb = baseColor;
	out_Color.a = 0.0;
}