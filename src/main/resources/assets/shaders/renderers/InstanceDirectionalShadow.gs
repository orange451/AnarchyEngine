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

layout(triangles) in;
#if __VERSION__ >= 400
layout(invocations = 4) in;
#endif
layout(triangle_strip, max_vertices = 12) out;

uniform mat4 projectionMatrix[4];

void compute(int i) {
	gl_Layer = i;
	gl_Position = projectionMatrix[i] * gl_in[0].gl_Position;
	EmitVertex();

	gl_Position = projectionMatrix[i] * gl_in[1].gl_Position;
	EmitVertex();

	gl_Position = projectionMatrix[i] * gl_in[2].gl_Position;
	EmitVertex();

	EndPrimitive();
}

void main() {
#if __VERSION__ >= 400
	compute(gl_InvocationID);
#else
	for (int i = 0; i < 6; i++)
		compute(i);
#endif
}