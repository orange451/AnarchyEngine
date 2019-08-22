#version 330

uniform mat4 uInverseViewMatrix;

vec3 reflectEnv( vec3 viewSpacePos, vec3 surfaceNormal ) {
	vec4 reflected = uInverseViewMatrix * vec4( reflect( viewSpacePos, surfaceNormal ), 0.0 );
	reflected.xyz = reflected.xzy; // Swizzle for z-up
	reflected.z *= -1;
	
	return reflected.xyz;
}