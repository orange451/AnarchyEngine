/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

layout(triangles) in;
layout(triangle_strip, max_vertices = 3) out;

in vec2 pass_textureCoords[];
in mat3 TBN[];

flat out int pass_axis;
flat out vec4 pass_aabb;
out vec3 pass_pos;

out vec3 pass_position_fs;
out vec2 pass_textureCoords_fs;
out mat3 TBN_fs;

uniform mat4 projection;
uniform mat4 viewX;
uniform mat4 viewY;
uniform mat4 viewZ;
uniform mat4 transformationMatrix;

const int size = 256;

void main() {
	vec3 normal = normalize(cross(gl_in[1].gl_Position.xyz - gl_in[0].gl_Position.xyz,
								  gl_in[2].gl_Position.xyz - gl_in[0].gl_Position.xyz));
	float NdotXAxis = abs(normal.x);
	float NdotYAxis = abs(normal.y);
	float NdotZAxis = abs(normal.z);
	mat4 view;

	if (NdotXAxis > NdotYAxis && NdotXAxis > NdotZAxis) {
		view = viewX;
		pass_axis = 1;
	} else if (NdotYAxis > NdotXAxis && NdotYAxis > NdotZAxis) {
		view = viewY;
		pass_axis = 2;
	} else {
		view = viewZ;
		pass_axis = 3;
	}
	vec4 worldPosition;
	vec4 positionRelativeToCam;

	vec4 pos[3];

	positionRelativeToCam = view * gl_in[0].gl_Position;
	pos[0] = positionRelativeToCam * projection;

	positionRelativeToCam = view * gl_in[1].gl_Position;
	pos[1] = positionRelativeToCam * projection;

	positionRelativeToCam = view * gl_in[2].gl_Position;
	pos[2] = positionRelativeToCam * projection;

	/*vec4 AABB;
	vec2 hPixel = vec2(1.0 / size, 1.0 / size);
	float pl = 1.4142135637309 / size;

	// calculate AABB of this triangle
	AABB.xy = pos[0].xy;
	AABB.zw = pos[0].xy;

	AABB.xy = min(pos[1].xy, AABB.xy);
	AABB.zw = max(pos[1].xy, AABB.zw);

	AABB.xy = min(pos[2].xy, AABB.xy);
	AABB.zw = max(pos[2].xy, AABB.zw);

	// Enlarge half-pixel
	AABB.xy -= hPixel;
	AABB.zw += hPixel;

	pass_aabb = AABB;

	// find 3 triangle edge plane
	vec3 e0 = vec3(pos[1].xy - pos[0].xy, 0);
	vec3 e1 = vec3(pos[2].xy - pos[1].xy, 0);
	vec3 e2 = vec3(pos[0].xy - pos[2].xy, 0);
	vec3 n0 = cross(e0, vec3(0, 0, 1));
	vec3 n1 = cross(e1, vec3(0, 0, 1));
	vec3 n2 = cross(e2, vec3(0, 0, 1));

	// dilate the triangle
	pos[0].xy = pos[0].xy + pl * ((e2.xy / dot(e2.xy, n0.xy)) + (e0.xy / dot(e0.xy, n2.xy)));
	pos[1].xy = pos[1].xy + pl * ((e0.xy / dot(e0.xy, n1.xy)) + (e1.xy / dot(e1.xy, n0.xy)));
	pos[2].xy = pos[2].xy + pl * ((e1.xy / dot(e1.xy, n2.xy)) + (e2.xy / dot(e2.xy, n1.xy)));*/

	// gl_Position = proj * gl_in[0].gl_Position;
	gl_Position = pos[0];
	pass_pos = pos[0].xyz;

	pass_position_fs = gl_in[0].gl_Position.xyz;
	pass_textureCoords_fs = pass_textureCoords[0];
	TBN_fs = TBN[0];
	EmitVertex();

	// gl_Position = proj * gl_in[1].gl_Position;
	gl_Position = pos[1];
	pass_pos = pos[1].xyz;

	pass_position_fs = gl_in[1].gl_Position.xyz;
	pass_textureCoords_fs = pass_textureCoords[1];
	TBN_fs = TBN[1];
	EmitVertex();

	// gl_Position = proj * gl_in[2].gl_Position;
	gl_Position = pos[2];
	pass_pos = pos[2].xyz;

	pass_position_fs = gl_in[2].gl_Position.xyz;
	pass_textureCoords_fs = pass_textureCoords[2];
	TBN_fs = TBN[2];
	EmitVertex();

	EndPrimitive();
}