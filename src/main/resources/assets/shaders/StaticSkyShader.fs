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

in vec3 pass_textureCoords;
in vec3 pass_position;

out vec4[5] out_Color;

uniform sampler2D environmentMap;
uniform float power;
uniform float brightness;

#include variable MASK

const vec2 invAtan = vec2(0.1591, 0.3183);

vec2 SampleSphericalMap(vec3 v) {
	vec2 uv = vec2(atan(v.z, v.x), asin(v.y));
	uv *= invAtan;
	uv += 0.5;
	return uv;
}

void main() {
	vec2 uv = SampleSphericalMap(normalize(pass_textureCoords));
	vec3 color = texture(environmentMap, uv).rgb;

	color = max(color, 0.0);
	color = pow(color, vec3(power));
	color *= brightness;

	out_Color[0].rgb = color;
	out_Color[0].a = 1.0;
	out_Color[1] = vec4(pass_position, 0.0);
	out_Color[2] = vec4(0.0);
	out_Color[3] = vec4(0.0);
	out_Color[4] = vec4(0.0, 0.0, 0.0, PBR_BACKGROUND);
}