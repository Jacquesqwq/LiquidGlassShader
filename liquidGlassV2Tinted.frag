/**
 * @author Jacques
 * @since 2026/06/03
 */

#version 120

uniform sampler2D uBlurTex;

uniform float uPowerFactor;
uniform float uNoise;
uniform float uRefractionPower;
uniform vec3 uTintColor;
uniform float uTintStrength;
uniform float uChromaStrength;
uniform float uDarkness;

varying vec2 vMidPointNDC;
varying vec2 vLocalOffsetNDC;

const float M_E = 2.718281828459045;
const vec2 CENTER = vec2(0.5);

float f(float x) {
    return 1.0 - 2.3 * pow(5.2 * M_E, -6.9 * x - 0.7);
}

float sdSuperellipse(vec2 p, float n, float r) {
    vec2 absP = abs(p);
    float numerator = pow(absP.x, n) + pow(absP.y, n) - pow(r, n);
    float denominator = n * sqrt(pow(absP.x, 2.0 * n - 2.0) + pow(absP.y, 2.0 * n - 2.0)) + 0.00001;
    return numerator / denominator;
}

bool OutOfBounds(vec2 uv) { return max(uv.x, uv.y) > 1.0 || min(uv.x, uv.y) < 0.0; }

void main() {
    vec2 localUV = gl_TexCoord[0].xy;

    vec2 p = (localUV - CENTER) * 2.0;

    float d = sdSuperellipse(p, uPowerFactor, 1.0);

    float edge = 1.0 - smoothstep(-0.003, 0.003, d);

    if (edge <= 0.0) discard;

    // Material sampling.
    float dist = clamp(max(-d, 0.0), 0.0, 1.0);
    float fresnel = pow(1.0 - dist, 3.0);

    float refraction = pow(f(dist), uRefractionPower);

    vec2 sampleUV = (vMidPointNDC + vLocalOffsetNDC * refraction) * 0.5 + vec2(0.5);

    if (OutOfBounds(sampleUV)) discard;

    // Subtle chromatic softness.
    vec2 chromaDir = normalize(vLocalOffsetNDC + 0.00001);
    vec2 chromaOffset = chromaDir * fresnel * uChromaStrength;

    vec4 color = vec4(
            texture2D(uBlurTex, sampleUV + chromaOffset).r,
            texture2D(uBlurTex, sampleUV).g,
            texture2D(uBlurTex, sampleUV - chromaOffset).b,
            1.0
    );

    // Low-frequency material diffusion.
    float micro = sin(gl_FragCoord.x * 0.015 + gl_FragCoord.y * 0.008) * sin(gl_FragCoord.y * 0.012 - gl_FragCoord.x * 0.006);

    color.rgb += micro * uNoise * 0.015;

    // Material shaping.
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    float grain = (micro - 0.5) * uNoise * 0.22;

    color.rgb += grain;

    vec3 grayscale = vec3(luma);

    float saturation = mix(0.45, 0.75, luma);

    color.rgb = mix(grayscale, color.rgb, saturation);

    vec3 adaptiveTintColor = mix(uTintColor, vec3(0.08, 0.09, 0.11), uDarkness);

    float adaptiveTint = uTintStrength * (1.0 - luma * 0.5);

    color.rgb = mix(color.rgb, adaptiveTintColor, adaptiveTint);

    float adaptiveFresnel = mix(0.12, 0.06, luma);

    color.rgb += uTintColor * fresnel * adaptiveFresnel;

    color.rgb *= 0.96 + smoothstep(0.0, 1.0, dist) * 0.04;

    color.rgb *= mix(1.08, 0.98, luma) * mix(1.0, 0.82, uDarkness);

    // Optical edge falloff.
    float opticalEdge = pow(edge, 1.35);
    color.rgb *= opticalEdge;

    gl_FragColor = vec4(color.rgb, opticalEdge);
}


