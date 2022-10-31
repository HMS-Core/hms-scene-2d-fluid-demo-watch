precision lowp float;
uniform sampler2D texture;
varying vec2 uv1;

void main() {
    gl_FragColor = texture2D(texture, uv1);
    gl_FragColor.a = 1.0;
}