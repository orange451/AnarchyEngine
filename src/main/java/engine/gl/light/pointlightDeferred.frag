#version 150 core

uniform sampler2D texture_albedo;
uniform sampler2D texture_depth;
uniform sampler2D texture_normal;
uniform sampler2D texture_pbr;

uniform vec2 texel;
uniform vec3 lightPosition;
uniform mat4 inverseProjectionMatrix;

uniform float radius;
uniform float intensity;
uniform vec3 lightColor;

in vec4 passColor;
in vec2 passTexCoord;
in vec3 passNormal;

out vec4 out_Color;

vec3 pointLight( vec3 N, vec3 eyeSpace, vec3 lightPosition, vec3 albedo, float metallic, float roughness, float radius, vec3 lightColor, float intensity );

void main(void) {
	vec2 texCoords = gl_FragCoord.xy * texel;
	vec4 clipPos = vec4(vec3(texCoords, texture(texture_depth, texCoords).r) * 2.0 - 1.0, 1.0);
	vec4 eyeSpace = inverseProjectionMatrix * clipPos;
	eyeSpace.xyz /= eyeSpace.w;

	vec3 N = texture( texture_normal, texCoords ).rgb;
	vec3 PBR = texture( texture_pbr, texCoords ).rgb;
	float metallic = PBR.x;
	float roughness = PBR.y;
	vec3 albedo = texture(texture_albedo, texCoords).rgb;
	
	vec3 color = pointLight( N, eyeSpace.xyz, lightPosition, albedo, metallic, roughness, radius, lightColor, intensity );
	
	out_Color = vec4( color, 1.0 );
}
