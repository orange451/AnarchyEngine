#version 330

uniform sampler2D texture_depth;
uniform sampler2D texture_diffuse;
uniform sampler2D texture_normal;
uniform sampler2D texture_pbr;
uniform samplerCube texture_ibl;

uniform mat4 uInverseViewMatrix;
uniform mat4 uInverseProjectionMatrix;

in vec2 passTexCoord;
in vec4 passColor;

out vec4 outColor;

const float MULTIPLIER = 1.0; // Test just to boost color to make sure I have output
const float MAX_REFLECTION_LOD = 14.0;

void main(void) {
	float depth = texture( texture_depth, passTexCoord ).r;
	if ( depth == 1.0 )
		discard;
		
	vec4 clipPos = vec4(vec3(passTexCoord, depth) * 2.0 - 1.0, 1.0);
	vec4 eyeSpace = uInverseProjectionMatrix * clipPos;
	eyeSpace.xyz /= eyeSpace.w;
	
	vec3 L = normalize(eyeSpace.xyz);
	vec3 N = texture( texture_normal, passTexCoord ).rgb;
	vec3 PBR = texture( texture_pbr, passTexCoord ).rgb;
	vec3 kD = texture( texture_diffuse, passTexCoord ).rgb;
	
	// Some PBR stuff
	float metallic = PBR.x;
	float roughness = PBR.y;
	kD *= 1.0 - metallic;
	
	// Calculate IBL radiance 
	vec3 radiance = textureLod( texture_ibl, reflect( L, N ), MAX_REFLECTION_LOD * roughness ).rgb;
	radiance *= MULTIPLIER;
	
	//outColor = vec4( (kD * radiance)+radiance, 1.0 );
	outColor = vec4( radiance, 1.0 );
}