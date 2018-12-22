#version 150

vec3 reflectEnv( vec3 viewSpacePos, vec3 surfaceNormal );

vec3 calculateFresnel( vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float metalness ) {
	float dim = 1.0 / 32.0;
	float power = 2.0 + ( 1.0 / ( roughness * 3.0 ) );
	float s = (1.0-metalness+0.125);
	
	// Calculate rim light
	float f = dot(viewSpacePos, surfaceNormal ) + 1.0;
	f = pow( f, power );
	f = f * s * dim;
	f = clamp( f, 0.0, 1.0 );
	vec3 fresnel = vec3(f);
	
	return fresnel;
}
