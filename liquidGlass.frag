/**
 * @author Jacques
 * @since 2026/05/17
 */

#version 120

uniform sampler2D uBlurTex;

uniform vec2 uResolution;
uniform vec2 uGlassPixelSize;

uniform float uPower;
uniform float uNoise;
uniform float uRefractionPower;

varying vec2 vMidPoint;
varying vec2 vScreenScale;

const float M_E = 2.718281828459045;
const vec2 CENTER = vec2(0.5);

// Stable screen-space grain.
float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

// Refraction response curve.
float f(float x) {
    return 1.0 - 2.3 * pow(5.2 * M_E, -6.9 * x - 0.7);
}

// Signed distance for the rounded glass shape.
float sdSuperellipse(vec2 p, float n, float r) {
    vec2 pAbs = abs(p);
    float numerator = pow(pAbs.x, n) + pow(pAbs.y, n) - pow(r, n);
    float denominator = n * sqrt(pow(pAbs.x, 2.0 * n - 2.0) + pow(pAbs.y, 2.0 * n - 2.0)) + 0.00001;
    return numerator / denominator;
}

float Glow(vec2 uv) {
    vec2 glowUV = uv * 2.0 - 1.0;
    return sin(atan(glowUV.y, glowUV.x) - 0.5);
}

void main() {
    vec2 localUV = gl_TexCoord[0].xy;
    vec2 p = (localUV - CENTER) * 2.0;

    // Glass shape.
    float d = sdSuperellipse(p, uPower, 1.0);

    float aa = max(fwidth(d) * 1.5, 1.2 / min(uGlassPixelSize.x, uGlassPixelSize.y));

    float edge = 1.0 - smoothstep(0.0, aa, d);

    if (edge <= 0.0) discard;

    float dist = max(-d, 0.0);

    // Refraction.
    vec2 sampleP = p * pow(f(dist), uRefractionPower);

    vec2 sampleUV = vMidPoint + vec2(sampleP.x, -sampleP.y) * vScreenScale * 0.5;

    vec2 safeUV = clamp(sampleUV, 0.003, 0.997);

    // Chromatic aberration.
    float chromaStrength = smoothstep(0.0, 0.35, dist);
    vec2 edgeDist = min(sampleUV, 1.0 - sampleUV);
    // Softer radial-style edge attenuation.
    float edgeFade = length(edgeDist * 2.0);
    edgeFade = smoothstep(0.0, 0.12, edgeFade);
    // Prevent edge streaking from large refraction vectors.
    float refractionFade = 1.0 - smoothstep(0.6, 1.4, length(sampleP));
    vec2 chromaOffset = normalize(sampleP + 0.00001) * chromaStrength * edgeFade * refractionFade * 0.0019;

    vec2 rUV = clamp(safeUV + chromaOffset, 0.003, 0.997);
    vec2 gUV = safeUV;
    vec2 bUV = clamp(safeUV - chromaOffset, 0.003, 0.997);

    // Fade chromatic aberration when channels begin diverging near screen edges.
    float chromaClipFade = 1.0 - smoothstep(0.0, 0.0025, distance(rUV, safeUV)) - smoothstep(0.0, 0.0025, distance(bUV, safeUV));

    chromaClipFade = clamp(chromaClipFade, 0.0, 1.0);

    rUV = mix(safeUV, rUV, chromaClipFade);
    bUV = mix(safeUV, bUV, chromaClipFade);

    vec4 color;
    color.r = texture2D(uBlurTex, rUV).r;
    color.g = texture2D(uBlurTex, gUV).g;
    color.b = texture2D(uBlurTex, bUV).b;
    color.a = 1.0;

    // Noise.
    float noise = (rand(gl_FragCoord.xy * 1e-3) - 0.5) * uNoise;

    // Subtle glass grain.
    color.rgb += vec3(noise);

    // Directional rim lighting.
    float glow = Glow(localUV);

    float glowMask = smoothstep(0.06, 0.0, dist);

    float glowStrength = glow * 0.3 * glowMask + 1.0;

    color.rgb *= glowStrength;

    color.rgb += smoothstep(0.85, 0.0, dist) * 0.032;

    gl_FragColor = vec4(color.rgb, edge * edge);
}
