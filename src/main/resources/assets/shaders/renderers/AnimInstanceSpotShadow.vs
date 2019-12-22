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
layout(location = 4) in vec4 boneIndices;
layout(location = 5) in vec4 boneWeights;

#define MAX_BONES 128

uniform mat4 transformationMatrix;
uniform mat4 viewMatrix;

uniform mat4 boneMat[MAX_BONES];

uniform mat4 projectionMatrix;

void main() {
	mat4 boneTransform = boneMat[int(boneIndices[0])] * boneWeights[0];
	boneTransform += boneMat[int(boneIndices[1])] * boneWeights[1];
	boneTransform += boneMat[int(boneIndices[2])] * boneWeights[2];
	boneTransform += boneMat[int(boneIndices[3])] * boneWeights[3];

	vec4 BToP = boneTransform * vec4(position, 1.0);

	vec4 worldPosition = transformationMatrix * BToP;
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	gl_Position = projectionMatrix * positionRelativeToCam;;
}