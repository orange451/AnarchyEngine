#version 150

mat3 cotangent_frame( vec3 N, vec3 p, vec2 uv ) {
    // get edge vectors of the pixel triangle
    vec3 dp1 = dFdx( p );
    vec3 dp2 = dFdy( p );
    vec2 duv1 = dFdx( uv );
    vec2 duv2 = dFdy( uv );
 
    // solve the linear system
    vec3 dp2perp = cross( dp2, N );
    vec3 dp1perp = cross( N, dp1 );
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
 
    // construct a scale-invariant frame 
    float invmax = inversesqrt( max( dot(T,T), dot(B,B) ) );
    T *= invmax;
    B *= invmax;
    
    return mat3( T, B, N );   
}

vec3 perturb_normal( vec3 N, vec3 V, vec2 texcoord, vec3 norm ) {
	vec3 map = vec3( norm.x, norm.y, 1.0 );
    map = map * 255.0/127.0 - 128.0/127.0;
    map.z = sqrt( 1.0 - dot( map.xx, map.yy ) );
    map.y = -map.y;
    mat3 TBN = cotangent_frame( N, V, texcoord );
    return normalize( TBN * map );
}


vec3 normalmap(vec3 normalSample, vec3 vNormal, vec3 vViewSpacePos, vec2 vTexCoords ) {
	vec3 N = normalize( vNormal );
	vec3 V = normalize( vViewSpacePos );
	return perturb_normal( N, V, vTexCoords.xy, normalSample );
}