#version 150

uniform sampler2D texture_diffuse;

in vec4 passColor;
in vec2 passTexCoord;

uniform float uExposure;
uniform float uGamma;
uniform float uSaturation;


const float A = 0.15;
const float B = 0.50;
const float C = 0.10;
const float D = 0.20;
const float E = 0.02;
const float F = 0.30;
const float W = 2.0;

out vec4 fragColor;

vec3 toneMap(vec3 x) {
   return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F)) - E/F;
}

float luma(vec3 color){
	vec3 cc = vec3(0.299, 0.587, 0.114);
	return dot(color, cc);
}

float rand(vec2 co){
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

float dither(){
	return (-0.5 + rand(passTexCoord)) * (1.0 / 255.0); //Noise dithering
}


void main(){
	vec3 c = toneMap(texture(texture_diffuse, passTexCoord).rgb * uExposure);
	
	vec3 whiteScale = 1.0/toneMap(vec3(W));
	c = pow(c*whiteScale, vec3(uGamma));
	float lum = luma(c);
	c += dither()/2.0;
	
	
	fragColor = vec4(mix(vec3(lum), c, uSaturation), 1.0);
	//fragColor = vec4(c, 1.0);
}
