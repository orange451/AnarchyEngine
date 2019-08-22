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

in vec3 passPositionOut;
in vec4 clipSpaceOut;
in vec2 textureCoordsOut;
in vec3 normal;

out vec4[5] out_Color;

uniform vec3 cameraPosition;
uniform sampler2D dudv;
uniform sampler2D foamMask;
uniform float time;

void main() {
	out_Color[0] = vec4(0.0313725490196078, 0.0549019607843137, 0.1568627450980392, 0.0);
	out_Color[1] = vec4(passPositionOut.xyz, 0.0);
	out_Color[2] = vec4(normal.xyz, 0.0);
	out_Color[3] = vec4(0.0, 0, 0.0, 0.0);
	out_Color[4] = vec4(0.0);
}