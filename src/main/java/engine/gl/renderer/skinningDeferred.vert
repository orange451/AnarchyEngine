#version 330

const int MAX_BONES = 128;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 worldMatrix;
uniform mat3 worldNormalMatrix;
uniform mat3 normalMatrix;

uniform vec4 materialColor;

uniform mat4 boneMat[MAX_BONES];

layout(location = 0) in vec3 inPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTexCoord;
layout(location = 3) in vec4 inColor;
layout(location = 4) in vec4 BoneIndices;
layout(location = 5) in vec4 BoneWeights;

out vec2 passTexCoord;
out vec4 passColor;
out vec3 passNormal;
out vec3 vViewSpacePos;
out vec3 vViewSpaceNor;

void main(void) {
    mat4 BoneTransform = boneMat[int(BoneIndices[0])] * BoneWeights[0];
    	BoneTransform += boneMat[int(BoneIndices[1])] * BoneWeights[1];
    	BoneTransform += boneMat[int(BoneIndices[2])] * BoneWeights[2];
    	BoneTransform += boneMat[int(BoneIndices[3])] * BoneWeights[3];
    
    vec4 PosL = BoneTransform * vec4(inPos, 1.0);
    vec4 NorL = BoneTransform * vec4(inNormal, 0.0);

	mat4 viewWorldMatrix = viewMatrix * worldMatrix;
	vec4 viewPos = viewWorldMatrix * PosL;
	vec4 viewNor = viewWorldMatrix * NorL;
	
	gl_Position = projectionMatrix * viewPos;
	passNormal = normalize(normalMatrix * worldNormalMatrix * inNormal);
	
	passTexCoord = inTexCoord;
	passColor = inColor;
	vViewSpacePos = viewPos.xyz;
	vViewSpaceNor = viewNor.xyz;
}
