#version 150

in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float SkyblockerTime;

out vec4 fragColor;

vec3 hsv2rgb_smooth(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    rgb = rgb * rgb * (3.0 - 2.0 * rgb); // Cubic smoothing - smooths out colour transitions
    return c.z * mix(vec3(1.0), rgb, c.y);
}

void main() {
    vec4 color = vertexColor;

    if (color.a == 0.0) {
        discard;
    }

    vec2 uv = gl_FragCoord.xy / ScreenSize.xy; // Normalize coordinates to range [0, 1]
    float offset = SkyblockerTime * 4.0; // Adjust the speed of the animation

    // Move the gradient horizontally from the top left to the bottom right
    uv.x -= uv.y;
    uv.y = 0.0;

    float h = mix(0.1, 0.15, mod(offset + -uv.x * 1.75, 1.0)); // Vary the hue
    float s = 0.75; // Keep saturation constant at 0.75 for 3/4 saturation
    float v = 1.0; // Keep value constant at 1.0 for full brightness

    vec3 hsv = vec3(h, s, v);
    vec3 rgb = hsv2rgb_smooth(hsv);

    color = vec4(rgb, color.a);

    fragColor = color * ColorModulator;
}
