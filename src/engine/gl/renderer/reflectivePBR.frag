#version 330

float getReflectionIndex( vec3 viewSpacePos, vec3 surfaceNormal, float reflective ) {
	// Calculate dot from surface->light from range [0-1]
	float f = abs( dot( viewSpacePos, surfaceNormal ) );

	// Limit reflectiveness and apply user-defined reflective value
	f = clamp( f - reflective, 0.0, 1.0 );

	// Apply power
	f = 1.0 - pow( 1.0 - f, 1.5 - reflective );
	
	// Return
	return f;
}

vec3 reflectivePBR( vec3 cubemapSample, vec3 viewSpacePos, vec3 surfaceNormal, float roughness, float reflective ) {

	// Get reflection index [0-1]
	float f = getReflectionIndex( viewSpacePos, surfaceNormal, reflective );

	// Combine reflective with cubemap sample
	return mix( cubemapSample, vec3(1.0), clamp( f, 0.0, 1.0 ) );
}