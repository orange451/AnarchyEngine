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