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

out vec3 out_Color;

uniform vec2 resolution;
uniform sampler2D image;

uniform int useChromaticAberration;

const float max_distort = 0.04;
const int num_iter = 12;
const float reci_num_iter_f = 1.0 / float(num_iter);

vec2 barrelDistortion(vec2 coord, float amt) {
	vec2 cc = coord - 0.5;
	float dist = dot(cc, cc);
	return coord + cc * dist * amt;
}

float sat(float t) {
	return clamp(t, 0.0, 1.0);
}

float linterp(float t) {
	return sat(1.0 - abs(2.0 * t - 1.0));
}

float remap(float t, float a, float b) {
	return sat((t - a) / (b - a));
}

vec3 spectrum_offset(float t) {
	vec3 ret;
	float lo = step(t, 0.5);
	float hi = 1.0 - lo;
	float w = linterp(remap(t, 1.0 / 6.0, 5.0 / 6.0));
	ret = vec3(lo, 1.0, hi) * vec3(1.0 - w, w, 1.0 - w);

	return pow(ret, vec3(1.0 / 2.2));
}

void main() {
	vec3 color = vec3(0.0);
	if (useChromaticAberration == 1) {
		vec3 sumcol = vec3(0.0);
		vec3 sumw = vec3(0.0);
		for (int i = 0; i < num_iter; ++i) {
			float t = float(i) * reci_num_iter_f;
			vec3 w = spectrum_offset(t);
			sumw += w;
			sumcol +=
				w * texture(image, barrelDistortion(textureCoords, 0.6 * max_distort * t)).rgb;
		}

		color = sumcol / sumw;
	} else {
		color = texture(image, textureCoords).rgb;
	}
	out_Color = color;
}