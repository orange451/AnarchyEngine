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