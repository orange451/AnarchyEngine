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

const float MULTIPLIER = 0.2;
const float CUTOFF = 0.4;
const float MAX_REFLECTION_LOD = 14.0;

vec3 reflectEnv( vec3 viewSpacePos, vec3 surfaceNormal );

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
	float reflectiveness = PBR.z;
	kD *= 1.0 - metallic;
	
	// Calculate IBL radiance
	vec3 radiance = textureLod( texture_ibl, reflectEnv( L, N ), MAX_REFLECTION_LOD * roughness ).rgb;
	
	// Correct radiance (boost brightness)
	radiance = MULTIPLIER >= 1 ? ((radiance - CUTOFF) * MULTIPLIER) + CUTOFF : radiance * MULTIPLIER;
	radiance = max( radiance, 0.0 );
	
	// Calculate final IBL based on reflectiveness
	vec3 final = mix( kD + radiance, (kD * radiance) + radiance * 0.25, reflectiveness );

	// Write
	outColor = vec4( final, 1.0 );
}