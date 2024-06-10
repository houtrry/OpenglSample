attribute vec4 inputTextureCoordinate;

varying vec2 textureCoordinate;

attribute vec4 vPosition;

void main() {
    gl_Position = vPosition;
    textureCoordinate = inputTextureCoordinate.xy;
}