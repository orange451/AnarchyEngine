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
layout(triangle_strip, max_vertices = 3) out;

in vec3 passPosition[];
in vec4 clipSpace[];
in vec2 passTextureCoords[];

out vec3 passPositionOut;
out vec4 clipSpaceOut;
out vec3 normal;
out vec2 textureCoordsOut;

vec3 calculateTriangleNormal() {
	vec3 tangent = passPosition[1].xyz - passPosition[0].xyz;
	vec3 bitangent = passPosition[2].xyz - passPosition[0].xyz;
	vec3 normal = cross(tangent, bitangent);
	return normalize(normal);
}

void main() {

	normal = calculateTriangleNormal();
	passPositionOut = passPosition[0];
	clipSpaceOut = clipSpace[0];
	textureCoordsOut = passTextureCoords[0];
	gl_Position = gl_in[0].gl_Position;
	EmitVertex();

	passPositionOut = passPosition[1];
	clipSpaceOut = clipSpace[1];
	textureCoordsOut = passTextureCoords[1];
	gl_Position = gl_in[1].gl_Position;
	EmitVertex();

	passPositionOut = passPosition[2];
	clipSpaceOut = clipSpace[2];
	textureCoordsOut = passTextureCoords[2];
	gl_Position = gl_in[2].gl_Position;
	EmitVertex();

	EndPrimitive();
}