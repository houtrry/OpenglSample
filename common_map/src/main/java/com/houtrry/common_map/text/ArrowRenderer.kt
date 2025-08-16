package com.houtrry.common_map.text

import android.opengl.GLES20
import com.houtrry.common_map.utils.toBuffer
import java.nio.FloatBuffer

/**
 * 箭头三角形渲染器：单色，使用与 Sprite 相同的 MVP/混合状态。
 */
class ArrowRenderer {

	private var program = 0
	private var uMvp = 0
	private var aPos = 0
	private var uColor = 0

	private lateinit var vertexBuffer: FloatBuffer

	fun initialize() {
		if (program != 0) return
		val vs = """
		uniform mat4 uMVPMatrix;
		attribute vec4 vPosition;
		void main(){
			gl_Position = uMVPMatrix * vPosition;
		}
		"""
		val fs = """
		precision mediump float;
		uniform vec4 uColor;
		void main(){ gl_FragColor = uColor; }
		"""
		program = createShaderProgram(vs, fs)
		uMvp = GLES20.glGetUniformLocation(program, "uMVPMatrix")
		aPos = GLES20.glGetAttribLocation(program, "vPosition")
		uColor = GLES20.glGetUniformLocation(program, "uColor")
	}

	fun draw(mvp: FloatArray, colorRgba: FloatArray, triangleNdc: FloatArray) {
		if (!::vertexBuffer.isInitialized || vertexBuffer.capacity() < triangleNdc.size) {
			vertexBuffer = triangleNdc.toBuffer()
		} else {
			vertexBuffer.clear(); vertexBuffer.put(triangleNdc).position(0)
		}

		GLES20.glUseProgram(program)
		GLES20.glEnable(GLES20.GL_BLEND)
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
		GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
		GLES20.glUniform4fv(uColor, 1, colorRgba, 0)

		GLES20.glEnableVertexAttribArray(aPos)
		GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
		GLES20.glDisableVertexAttribArray(aPos)
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


