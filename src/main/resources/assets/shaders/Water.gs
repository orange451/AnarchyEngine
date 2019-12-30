/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

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