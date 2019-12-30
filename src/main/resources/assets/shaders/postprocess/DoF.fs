/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

in vec2 textureCoords;

out vec3 out_Color;

uniform sampler2D image;
uniform sampler2D depth;

uniform int useDOF;

void main(void) {
	vec3 color = texture(image, textureCoords).rgb;
	if (useDOF == 1) {
		vec3 sum = color;
		float bias =
			min(abs(texture(depth, textureCoords).r - texture(depth, vec2(0.5)).r) * .01, .005);
		for (int i = -4; i < 4; i++) {
			for (int j = -4; j < 4; j++) {
				sum += texture(image, textureCoords + vec2(j, i) * bias).rgb;
			}
		}
		sum /= 65.0;
		color = sum;
	}
	out_Color = color;
}