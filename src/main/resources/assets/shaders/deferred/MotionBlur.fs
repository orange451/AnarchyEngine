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

uniform sampler2D image;

uniform sampler2D gMotion;

uniform int useMotionBlur;

void main() {
	vec3 textureColor = texture(image, textureCoords).rgb;
	if (useMotionBlur == 1) {
		vec2 vel = texture(gMotion, textureCoords).rg * 0.5;
		vec2 motionCoords = textureCoords;
		vec3 sum = textureColor;

		vel *= 0.1; // Controls the amount of blur
		int samples = 1;
		vec2 coord = textureCoords - vel * 6.0;
		for (int i = 0; i < 12; ++i, coord += vel) {
			sum += texture(image, coord).rgb;
			samples++;
		}
		sum = sum / samples;
		textureColor = sum;
	}
	out_Color.rgb = textureColor;
	out_Color.a = 0.0;
}