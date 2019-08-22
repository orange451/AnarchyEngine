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
uniform float exposure;

#include variable GLOBAL

void main() {
	vec3 color = texture(image, textureCoords).rgb;
	vec3 final = vec3(1.0) - exp(-color * exposure);
	out_Color.rgb = pow(final, vec3(1.0 / GAMMA));
	out_Color.a = 1;
}