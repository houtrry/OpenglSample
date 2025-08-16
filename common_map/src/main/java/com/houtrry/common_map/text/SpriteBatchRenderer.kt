package com.houtrry.common_map.text

import android.opengl.GLES20
import com.houtrry.common_map.utils.toBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 轻量 2D Sprite 批渲染器（ES2 兼容）：按纹理分组，一次绑定纹理、批量绘制多个四边形。
 * 仅提供最小能力：调用方负责准备每个实例的 NDC 顶点与 UV。
 */
class SpriteBatchRenderer {

	private var program = 0
	private var uMvp = 0
	private var aPos = 0
	private var aUv = 0
	private var uTex = 0

	private lateinit var vertexBuffer: FloatBuffer
	private lateinit var uvBuffer: FloatBuffer
	private lateinit var indexBuffer: ShortBuffer

	private val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

	fun initialize() {
		if (program != 0) return
		val vs = """
		uniform mat4 uMVPMatrix;
		attribute vec4 vPosition;
		attribute vec2 vTexCoord;
		varying vec2 texCoord;
		void main(){
			gl_Position = uMVPMatrix * vPosition;
			texCoord = vTexCoord;
		}
		"""
		val fs = """
		precision mediump float;
		uniform sampler2D uTexture;
		varying vec2 texCoord;
		void main(){
			gl_FragColor = texture2D(uTexture, texCoord);
		}
		"""
		program = createShaderProgram(vs, fs)
		uMvp = GLES20.glGetUniformLocation(program, "uMVPMatrix")
		aPos = GLES20.glGetAttribLocation(program, "vPosition")
		aUv = GLES20.glGetAttribLocation(program, "vTexCoord")
		uTex = GLES20.glGetUniformLocation(program, "uTexture")
		indexBuffer = indices.toBuffer()
	}

	fun begin(mvp: FloatArray) {
		GLES20.glUseProgram(program)
		GLES20.glEnable(GLES20.GL_BLEND)
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
		GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
	}

	fun draw(textureId: Int, quadVerticesNdc: FloatArray, quadUvs: FloatArray) {
		if (!::uvBuffer.isInitialized || uvBuffer.capacity() < quadUvs.size) {
			uvBuffer = quadUvs.toBuffer()
		} else {
			uvBuffer.clear(); uvBuffer.put(quadUvs).position(0)
		}
		if (!::vertexBuffer.isInitialized || vertexBuffer.capacity() < quadVerticesNdc.size) {
			vertexBuffer = quadVerticesNdc.toBuffer()
		} else {
			vertexBuffer.clear(); vertexBuffer.put(quadVerticesNdc).position(0)
		}

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
		GLES20.glUniform1i(uTex, 0)

		GLES20.glEnableVertexAttribArray(aPos)
		GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
		GLES20.glEnableVertexAttribArray(aUv)
		GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 0, uvBuffer)

		GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
	}

	fun end() {
		GLES20.glDisableVertexAttribArray(aPos)
		GLES20.glDisableVertexAttribArray(aUv)
		GLES20.glDisable(GLES20.GL_BLEND)
		GLES20.glUseProgram(0)
	}

	private fun loadShader(type: Int, shaderCode: String): Int {
		val shader = GLES20.glCreateShader(type)
		GLES20.glShaderSource(shader, shaderCode)
		GLES20.glCompileShader(shader)
		return shader
	}

	private fun createShaderProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
		val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
		val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
		val program = GLES20.glCreateProgram()
		GLES20.glAttachShader(program, vertexShader)
		GLES20.glAttachShader(program, fragmentShader)
		GLES20.glLinkProgram(program)
		return program
	}
}


