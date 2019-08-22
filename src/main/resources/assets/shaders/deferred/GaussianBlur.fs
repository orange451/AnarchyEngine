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

in vec2 blurTexCoords[17];

out vec4 out_Color;

uniform sampler2D composite0;

void main() {
	vec4 result = vec4(0.0);
	result += texture(composite0, blurTexCoords[0]) * 0.003924;
	result += texture(composite0, blurTexCoords[1]) * 0.008962;
	result += texture(composite0, blurTexCoords[2]) * 0.018331;
	result += texture(composite0, blurTexCoords[3]) * 0.033585;
	result += texture(composite0, blurTexCoords[4]) * 0.055119;
	result += texture(composite0, blurTexCoords[5]) * 0.081029;
	result += texture(composite0, blurTexCoords[6]) * 0.106701;
	result += texture(composite0, blurTexCoords[7]) * 0.125858;
	result += texture(composite0, blurTexCoords[8]) * 0.13298;
	result += texture(composite0, blurTexCoords[9]) * 0.125858;
	result += texture(composite0, blurTexCoords[10]) * 0.106701;
	result += texture(composite0, blurTexCoords[11]) * 0.081029;
	result += texture(composite0, blurTexCoords[12]) * 0.055119;
	result += texture(composite0, blurTexCoords[13]) * 0.033585;
	result += texture(composite0, blurTexCoords[14]) * 0.018331;
	result += texture(composite0, blurTexCoords[15]) * 0.008962;
	result += texture(composite0, blurTexCoords[16]) * 0.003924;
	out_Color = result;
}