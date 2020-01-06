/*

Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)

This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

 */

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec3 tangent;
layout(location = 3) in vec2 textureCoords;
layout(location = 4) in vec4 inColor;
layout(location = 5) in vec4 boneIndices;
layout(location = 6) in vec4 boneWeights;

#define MAX_BONES 64

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

uniform mat4 boneMat[MAX_BONES];
uniform mat4 boneMatPrev[MAX_BONES];

void main() {
	mat4 boneTransform = boneMat[int(boneIndices[0])] * boneWeights[0];
	boneTransform += boneMat[int(boneIndices[1])] * boneWeights[1];
	boneTransform += boneMat[int(boneIndices[2])] * boneWeights[2];
	boneTransform += boneMat[int(boneIndices[3])] * boneWeights[3];

	vec4 BToP = boneTransform * vec4(position, 1.0);
	vec4 BToN = boneTransform * vec4(normal, 0.0);
	vec4 BToT = boneTransform * vec4(tangent, 0.0);

	vec4 worldPosition = transformationMatrix * BToP;
	vec4 positionRelativeToCam = viewMatrix * worldPosition;
	clipSpace = projectionMatrix * positionRelativeToCam;
	if (useTAA) {
		gl_Position = jitterMatrix * clipSpace;
	} else {
		gl_Position = clipSpace;
	}
	pass_textureCoords = textureCoords;

	vec3 T = normalize(vec3(transformationMatrix * vec4(tangent, 0.0)));
	vec3 N = normalize(vec3(transformationMatrix * vec4(BToN.xyz, 0.0)));
	T = normalize(T - dot(T, N) * N);
	vec3 B = cross(N, T);
	TBN = mat3(T, B, N);

	pass_position = worldPosition.xyz;

	boneTransform = boneMatPrev[int(boneIndices[0])] * boneWeights[0];
	boneTransform += boneMatPrev[int(boneIndices[1])] * boneWeights[1];
	boneTransform += boneMatPrev[int(boneIndices[2])] * boneWeights[2];
	boneTransform += boneMatPrev[int(boneIndices[3])] * boneWeights[3];

	BToP = boneTransform * vec4(position, 1.0);

	worldPosition = transformationMatrixPrev * BToP;
	positionRelativeToCam = viewMatrixPrev * worldPosition;
	clipSpacePrev = projectionMatrixPrev * positionRelativeToCam;
}