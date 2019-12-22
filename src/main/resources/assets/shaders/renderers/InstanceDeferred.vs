/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 textureCoords;
layout(location = 3) in vec4 inColor;

out vec2 pass_textureCoords;
out vec3 pass_position;
out mat3 TBN;
out vec4 clipSpace;
out vec4 clipSpacePrev;

uniform mat4 transformationMatrix;
uniform mat4 transformationMatrixPrev;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrixPrev;
uniform mat4 viewMatrixPrev;
uniform mat4 jitterMatrix;
uniform int frame;

uniform bool useTAA;

void main() {
	vec4 worldPosition = transformationMatrix * vec4(position, 1.0);
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	clipSpace = projectionMatrix * positionRelativeToCam;
	if (useTAA) {
		gl_Position = jitterMatrix * clipSpace;
	} else {
		gl_Position = clipSpace;
	}

	pass_textureCoords = textureCoords;

	vec3 t;
	vec3 c1 = cross(normal, vec3(0.0, 0.0, 1.0));
	vec3 c2 = cross(normal, vec3(0.0, 1.0, 0.0));
	if (length(c1) > length(c2))
		t = c1;
	else
		t = c2;
	vec3 T = normalize(vec3(transformationMatrix * vec4(t, 0.0)));
	vec3 N = normalize(vec3(transformationMatrix * vec4(normal, 0.0)));
	T = normalize(T - dot(T, N) * N);
	vec3 B = cross(N, T);
	TBN = mat3(T, B, N);

	pass_position = worldPosition.xyz;

	worldPosition = transformationMatrixPrev * vec4(position, 1.0);
	positionRelativeToCam = viewMatrixPrev * worldPosition;
	clipSpacePrev = projectionMatrixPrev * positionRelativeToCam;
}