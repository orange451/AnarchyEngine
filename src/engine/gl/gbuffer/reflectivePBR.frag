#version 330

vec3 reflectivePBR( vec3 cubemapSample, vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float reflective ) {

	// Calculate dot from surface->light from range [0-1]
	float f = abs( dot( viewSpacePos, surfaceNormal ) );

	// Limit reflectiveness and apply user-defined reflective value
	f = clamp( f - reflective, 0.0, 1.0 );

	// Apply power
	f = 1.0 - pow( 1.0 - f, 1.25 );

	// Combine reflective with cubemap sample
	return mix( cubemapSample, vec3(1.0), clamp( f, 0.0, 1.0 ) );
}
