#version 330

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 worldMatrix;
uniform mat3 worldNormalMatrix;
uniform mat3 normalMatrix;

uniform vec4 materialColor;

layout(location = 0) in vec3 inPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTexCoord;
layout(location = 3) in vec4 inColor;

out vec2 passTexCoord;
out vec4 passColor;
out vec3 passNormal;
out vec3 vViewSpacePos;
out vec3 vViewSpaceNor;

void main(void) {
	mat4 viewWorldMatrix = viewMatrix * worldMatrix;
	vec4 viewPos = viewWorldMatrix * vec4(inPos, 1.0);
	vec4 viewNor = viewWorldMatrix * vec4(inNormal, 0.0);
	
	gl_Position = projectionMatrix * viewMatrix * worldMatrix * vec4(inPos,1.0);
	passNormal = normalize(normalMatrix * worldNormalMatrix * inNormal);
	
	passTexCoord = inTexCoord;
	passColor = inColor;
	vViewSpacePos = viewPos.xyz;
	vViewSpaceNor = viewNor.xyz;
}
