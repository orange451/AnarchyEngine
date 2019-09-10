

#function toneMap
const float A = 0.15;
const float B = 0.50;
const float C = 0.10;
const float D = 0.20;
const float E = 0.02;
const float F = 0.30;
const float W = 1.0;

vec3 toneMap(vec3 x) {
	return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}
#end

#function luma
float luma(vec3 color) {
	vec3 cc = vec3(0.299, 0.587, 0.114);
	return dot(color, cc);
}
#end