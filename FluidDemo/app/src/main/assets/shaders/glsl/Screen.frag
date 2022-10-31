precision lowp float;
uniform sampler2D canvasTexture;
uniform sampler2D texture;
uniform float alphaThreshold;
varying vec2 uv1;

void main() {
    vec4 canvasColor =  texture2D(canvasTexture, uv1);
    vec4 color =  texture2D(texture, uv1);
    color.a = step(alphaThreshold, color.a);
    if (color.a > 0.0) {
        gl_FragColor = color * alphaThreshold + canvasColor * (1.0 - alphaThreshold);
		gl_FragColor.a = step(alphaThreshold, gl_FragColor.a);
    } else {
		gl_FragColor = canvasColor;
	}
}