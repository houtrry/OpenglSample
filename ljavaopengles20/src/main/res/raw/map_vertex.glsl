attribute vec4 inputTextureCoordinate;

varying vec2 textureCoordinate;

attribute vec4 vPosition;
uniform mat4 u_TransformMatrix;

void main() {
    gl_Position = u_TransformMatrix * vPosition;
    textureCoordinate = inputTextureCoordinate.xy;
}