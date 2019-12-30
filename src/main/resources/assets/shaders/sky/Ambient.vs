/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

layout(location = 0) in vec3 position;

out vec3 pass_position;
out vec4 clipSpace;
out vec4 clipSpacePrev;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 transformationMatrix;
uniform mat4 projectionMatrixPrev;
uniform mat4 viewMatrixPrev;

void main() {
	vec4 worldPosition = transformationMatrix * vec4(position, 1.0);
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	clipSpace = projectionMatrix * positionRelativeToCam;
	gl_Position = clipSpace;
	pass_position = worldPosition.xyz;

	worldPosition = transformationMatrix * vec4(position, 1.0);
	positionRelativeToCam = viewMatrixPrev * worldPosition;
	clipSpacePrev = projectionMatrixPrev * positionRelativeToCam;
}