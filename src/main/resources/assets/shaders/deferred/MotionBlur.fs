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