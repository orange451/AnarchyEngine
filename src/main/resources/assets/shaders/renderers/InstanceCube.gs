/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

layout(triangles) in;
#if __VERSION__ >= 400
layout(invocations = 6) in;
#endif
layout(triangle_strip, max_vertices = 18) out;

in vec2 passTextureCoords[];
in vec4 passPosition[];
in mat3 passTBN[];

out vec2 pass_textureCoords;
out vec4 pass_position;
out mat3 TBN;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrixCube[6];

vec4 positionRelativeToCam;

void compute(int i) {
	gl_Layer = i;

	positionRelativeToCam = viewMatrixCube[i] * gl_in[0].gl_Position;
	pass_textureCoords = passTextureCoords[0];
	pass_position = passPosition[0];
	TBN = passTBN[0];
	gl_Position = projectionMatrix * positionRelativeToCam;
	EmitVertex();

	positionRelativeToCam = viewMatrixCube[i] * gl_in[1].gl_Position;
	pass_textureCoords = passTextureCoords[1];
	pass_position = passPosition[1];
	TBN = passTBN[1];
	gl_Position = projectionMatrix * positionRelativeToCam;
	EmitVertex();

	positionRelativeToCam = viewMatrixCube[i] * gl_in[2].gl_Position;
	pass_textureCoords = passTextureCoords[2];
	pass_position = passPosition[2];
	TBN = passTBN[2];
	gl_Position = projectionMatrix * positionRelativeToCam;
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