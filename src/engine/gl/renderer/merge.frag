#version 330

uniform sampler2D texture_albedo;
uniform sampler2D texture_emissive;
uniform sampler2D texture_accumulation;
uniform sampler2D texture_ssao;
uniform sampler2D texture_pbr;
uniform sampler2D texture_depth;
uniform sampler2D texture_transparency;
uniform samplerCube texture_skybox;

uniform mat4 inverseViewMatrix;
uniform mat4 inverseProjectionMatrix;
uniform mat3 uSkyBoxRotation;

uniform vec3 uAmbient;
uniform vec3 cameraPosition;

in vec2 passTexCoord;
in vec4 passColor;

out vec4 outColor;

void main(void) {
	vec3 albedoSample = texture(texture_albedo, passTexCoord).rgb;
	vec3 emissiveSample = texture(texture_emissive, passTexCoord).rgb;
	vec3 accumulationSample = texture(texture_accumulation, passTexCoord).rgb;
	vec4 pbrSample = texture(texture_pbr, passTexCoord);
	float rawDepth = texture(texture_depth, passTexCoord).r;
	
	if ( rawDepth < 1.0 ) {
		albedoSample = vec3(0.0);
	}
	
	// Combine light and diffuse
	vec3 diffuseLight = (clamp( 1.0 - accumulationSample, 0.0, 1.0 )) * (albedoSample * uAmbient) + accumulationSample;
	diffuseLight = max( diffuseLight, 0.0 );
	
	// Combine albedo and emissive
	diffuseLight += emissiveSample.rgb;
	
	// Calculate skybox
	vec4 clipPos = vec4(vec3(passTexCoord, 1.0) * 2.0 - 1.0, 1.0);
	vec4 eyeSpace = inverseProjectionMatrix * clipPos;
	eyeSpace.xyzw *= 1.0 / eyeSpace.w;
	vec4 worldSpace = inverseViewMatrix * eyeSpace;
	vec3 worldVector = worldSpace.xyz - cameraPosition;
	vec3 skybox = texture(texture_skybox, uSkyBoxRotation * worldVector).rgb;
	
	if ( rawDepth == 1.0 ) {
		//diffuseLight = skybox;
	}
	
	
	outColor = vec4( diffuseLight, 1.0 ) + texture(texture_transparency, passTexCoord);
}
