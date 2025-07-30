package com.houtrry.lopengles20.utils

import android.opengl.Matrix
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object MatrixUtils {
    private const val EPSILON = 1e-6f
    private const val INDEX_OUT_OF_BOUNDS_MSG = "Input matrix must have exactly 16 elements"
    private val IDENTITY_MATRIX = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    /**
     * 计算多个矩阵的乘积
     * @param result 结果矩阵 (可以传入null)
     * @param matrices 要相乘的矩阵数组 (顺序从左到右)
     * @return 乘积矩阵
     */
    fun multiplyMatrices(matrix: FloatArray, vararg matrices: FloatArray?): FloatArray {
        var result = matrix
        require(matrices.isNotEmpty()) { "至少需要一个矩阵" }
        var temp1 = FloatArray(16)
        var temp2 = FloatArray(16)

        // 初始化第一个矩阵
        System.arraycopy(matrices[0], 0, temp1, 0, 16)
        for (i in 1 until matrices.size) {
            Matrix.multiplyMM(temp2, 0, temp1, 0, matrices[i], 0)
            // 交换临时矩阵
            val swap = temp1
            temp1 = temp2
            temp2 = swap
        }
        if (result == null) {
            result = FloatArray(16)
        }
        System.arraycopy(temp1, 0, result, 0, 16)
        return result
    }

    /**
     * 从4x4变换矩阵中提取指定分量
     *
     * @param srcMatrix 输入矩阵(列主序，16个元素)
     * @param destMatrix 输出矩阵(列主序，16个元素)
     * @param useTranslate 是否包含平移分量
     * @param useRotate 是否包含旋转分量
     * @param useScale 是否包含缩放分量
     * @return 包含指定分量的矩阵
     * @throws IllegalArgumentException 如果输入矩阵无效
     */
    fun getComponentOfMatrix(
        srcMatrix: FloatArray,
        destMatrix: FloatArray,
        useTranslate: Boolean,
        useRotate: Boolean,
        useScale: Boolean
    ): FloatArray {
        // 1. 输入验证
        require(srcMatrix.size == 16) { INDEX_OUT_OF_BOUNDS_MSG }
        require(destMatrix.size == 16) { "Output matrix must have exactly 16 elements" }

        // 2. 初始化目标矩阵为单位矩阵
        System.arraycopy(IDENTITY_MATRIX, 0, destMatrix, 0, 16)

        // 3. 快速路径：不需要任何线性变换
        if (!useRotate && !useScale) {
            if (useTranslate) {
                destMatrix[12] = srcMatrix[12]
                destMatrix[13] = srcMatrix[13]
                destMatrix[14] = srcMatrix[14]
            }
            return destMatrix
        }

        // 4. 提取平移分量
        if (useTranslate) {
            destMatrix[12] = srcMatrix[12]
            destMatrix[13] = srcMatrix[13]
            destMatrix[14] = srcMatrix[14]
        }

        // 5. 提取3x3线性变换部分
        val linear3x3 = extract3x3Matrix(srcMatrix)

        // 6. 处理纯对角矩阵(只有缩放)
        if (isDiagonal(linear3x3, EPSILON)) {
            if (useScale) {
                destMatrix[0] = abs(linear3x3[0])
                destMatrix[5] = abs(linear3x3[4])
                destMatrix[10] = abs(linear3x3[8])
            }
            if (useRotate) {
                // 对角矩阵的旋转部分是单位矩阵或反射
                val signX = if (linear3x3[0] < 0) -1f else 1f
                val signY = if (linear3x3[4] < 0) -1f else 1f
                val signZ = if (linear3x3[8] < 0) -1f else 1f

                // 确保右手坐标系
                if (signX * signY * signZ < 0) {
                    destMatrix[8] = -destMatrix[8]
                    destMatrix[9] = -destMatrix[9]
                    destMatrix[10] = -destMatrix[10]
                }
            }
            return destMatrix
        }

        // 7. 使用改进的极分解
        try {
            val (rotation, scaling) = improvedPolarDecomposition(linear3x3)

            // 8. 处理缩放分量
            if (useScale) {
                destMatrix[0] = scaling[0]
                destMatrix[5] = scaling[1]
                destMatrix[10] = scaling[2]
            }

            // 9. 处理旋转分量
            if (useRotate) {
                for (i in 0..2) {
                    for (j in 0..2) {
                        destMatrix[j * 4 + i] = rotation[i * 3 + j]
                    }
                }
            }
        } catch (e: ArithmeticException) {
            // 数值不稳定时的回退策略
            if (useRotate) {
                // 保留原始矩阵的旋转"方向"，即使缩放不正确
                val fallbackRotation = extract3x3Matrix(IDENTITY_MATRIX)
                for (i in 0..2) {
                    for (j in 0..2) {
                        destMatrix[j * 4 + i] = fallbackRotation[i * 3 + j]
                    }
                }
            }
            if (useScale) {
                // 使用列向量的长度作为近似缩放
                destMatrix[0] = sqrt(linear3x3[0].pow(2) + linear3x3[1].pow(2) + linear3x3[2].pow(2))
                destMatrix[5] = sqrt(linear3x3[3].pow(2) + linear3x3[4].pow(2) + linear3x3[5].pow(2))
                destMatrix[10] = sqrt(linear3x3[6].pow(2) + linear3x3[7].pow(2) + linear3x3[8].pow(2))
            }
        }

        return destMatrix
    }

    // ============== 核心分解算法 ==============

    private fun improvedPolarDecomposition(m: FloatArray): Pair<FloatArray, FloatArray> {
        // 1. 计算 M^T * M
        val mtm = FloatArray(9).apply {
            for (i in 0..2) {
                for (j in 0..2) {
                    var sum = 0f
                    for (k in 0..2) {
                        sum += m[k * 3 + i] * m[k * 3 + j]
                    }
                    this[i * 3 + j] = sum
                }
            }
        }

        // 2. 计算对称矩阵 S = sqrt(M^T * M)
        val s = stableMatrixSqrt(mtm)

        // 3. 计算旋转 R = M * S^-1
        val invS = try {
            invertSymmetric3x3(s)
        } catch (e: ArithmeticException) {
            pseudoInvertSymmetric3x3(s)
        }

        val r = FloatArray(9).apply {
            for (i in 0..2) {
                for (j in 0..2) {
                    var sum = 0f
                    for (k in 0..2) {
                        sum += m[i * 3 + k] * invS[k * 3 + j]
                    }
                    this[i * 3 + j] = sum
                }
            }
        }

        // 4. 修正旋转矩阵
        val fixedR = orthogonalizeRotationMatrix(r)

        // 5. 从S中提取缩放因子
        val scale = floatArrayOf(abs(s[0]), abs(s[4]), abs(s[8]))

        return Pair(fixedR, scale)
    }

    // ============== 矩阵运算工具 ==============

    private fun extract3x3Matrix(m: FloatArray): FloatArray {
        return floatArrayOf(
            m[0], m[1], m[2],
            m[4], m[5], m[6],
            m[8], m[9], m[10]
        )
    }

    private fun isDiagonal(m: FloatArray, epsilon: Float): Boolean {
        for (i in 0..2) {
            for (j in 0..2) {
                if (i != j && abs(m[i * 3 + j]) > epsilon) {
                    return false
                }
            }
        }
        return true
    }

    private fun stableMatrixSqrt(m: FloatArray): FloatArray {
        if (isDiagonal(m, EPSILON)) {
            return FloatArray(9).apply {
                this[0] = sqrt(abs(m[0])).coerceAtLeast(EPSILON)
                this[4] = sqrt(abs(m[4])).coerceAtLeast(EPSILON)
                this[8] = sqrt(abs(m[8])).coerceAtLeast(EPSILON)
            }
        }

        var x = m.clone()
        var prevX: FloatArray

        repeat(10) {
            prevX = x.clone()
            val invX = invertSymmetric3x3(x)
            x = matrixAverage(x, matrixMultiply(m, invX))

            if (matrixFrobeniusNorm(matrixSubtract(x, prevX)) < EPSILON) {
                return x
            }
        }

        return x
    }

    private fun orthogonalizeRotationMatrix(m: FloatArray): FloatArray {
        val v0 = floatArrayOf(m[0], m[3], m[6])
        val v1 = floatArrayOf(m[1], m[4], m[7])
        val v2 = floatArrayOf(m[2], m[5], m[8])

        val e0 = normalize(v0, EPSILON)
        val u1 = subtract(v1, project(v1, e0))
        val e1 = normalize(u1, EPSILON)
        val u2 = subtract(subtract(v2, project(v2, e0)), project(v2, e1))
        val e2 = normalize(u2, EPSILON)

        val cross = crossProduct(e0, e1)
        if (dot(cross, e2) < 0) {
            for (i in e2.indices) {
                e2[i] = -e2[i]
            }
        }

        return FloatArray(9).apply {
            this[0] = e0[0]; this[1] = e1[0]; this[2] = e2[0]
            this[3] = e0[1]; this[4] = e1[1]; this[5] = e2[1]
            this[6] = e0[2]; this[7] = e1[2]; this[8] = e2[2]
        }
    }

    private fun invertSymmetric3x3(m: FloatArray): FloatArray {
        val det = determinant3x3(m)
        if (abs(det) < EPSILON) {
            throw ArithmeticException("Matrix is singular (non-invertible)")
        }

        val invDet = 1f / det
        val result = FloatArray(9)

        result[0] = (m[4] * m[8] - m[5] * m[7]) * invDet
        result[1] = (m[2] * m[7] - m[1] * m[8]) * invDet
        result[2] = (m[1] * m[5] - m[2] * m[4]) * invDet
        result[3] = result[1]
        result[4] = (m[0] * m[8] - m[2] * m[6]) * invDet
        result[5] = (m[2] * m[3] - m[0] * m[5]) * invDet
        result[6] = result[2]
        result[7] = result[5]
        result[8] = (m[0] * m[4] - m[1] * m[3]) * invDet

        return result
    }

    private fun pseudoInvertSymmetric3x3(m: FloatArray): FloatArray {
        val det = determinant3x3(m)
        return if (abs(det) > EPSILON) {
            invertSymmetric3x3(m)
        } else {
            val perturbed = m.clone()
            for (i in 0..2) {
                perturbed[i * 3 + i] += EPSILON
            }
            invertSymmetric3x3(perturbed)
        }
    }

    private fun determinant3x3(m: FloatArray): Float {
        return m[0] * (m[4] * m[8] - m[5] * m[7]) -
                m[1] * (m[3] * m[8] - m[5] * m[6]) +
                m[2] * (m[3] * m[7] - m[4] * m[6])
    }

    // ============== 向量运算工具 ==============

    private fun normalize(v: FloatArray, epsilon: Float = EPSILON): FloatArray {
        val len = sqrt(v[0].pow(2) + v[1].pow(2) + v[2].pow(2))
        return if (len > epsilon) {
            floatArrayOf(v[0]/len, v[1]/len, v[2]/len)
        } else {
            floatArrayOf(1f, 0f, 0f) // 默认X轴方向
        }
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2]
    }

    private fun crossProduct(a: FloatArray, b: FloatArray): FloatArray {
        return floatArrayOf(
            a[1]*b[2] - a[2]*b[1],
            a[2]*b[0] - a[0]*b[2],
            a[0]*b[1] - a[1]*b[0]
        )
    }

    private fun project(v: FloatArray, onto: FloatArray): FloatArray {
        val scale = dot(v, onto) / dot(onto, onto)
        return floatArrayOf(onto[0]*scale, onto[1]*scale, onto[2]*scale)
    }

    private fun subtract(a: FloatArray, b: FloatArray): FloatArray {
        return floatArrayOf(a[0]-b[0], a[1]-b[1], a[2]-b[2])
    }

    // ============== 矩阵运算工具 ==============

    private fun matrixMultiply(a: FloatArray, b: FloatArray): FloatArray {
        return FloatArray(9).apply {
            for (i in 0..2) {
                for (j in 0..2) {
                    var sum = 0f
                    for (k in 0..2) {
                        sum += a[i * 3 + k] * b[k * 3 + j]
                    }
                    this[i * 3 + j] = sum
                }
            }
        }
    }

    private fun matrixSubtract(a: FloatArray, b: FloatArray): FloatArray {
        return FloatArray(9).apply {
            for (i in indices) {
                this[i] = a[i] - b[i]
            }
        }
    }

    private fun matrixAverage(a: FloatArray, b: FloatArray): FloatArray {
        return FloatArray(9).apply {
            for (i in indices) {
                this[i] = (a[i] + b[i]) / 2f
            }
        }
    }

    private fun matrixFrobeniusNorm(m: FloatArray): Float {
        var sum = 0f
        for (value in m) {
            sum += value * value
        }
        return sqrt(sum)
    }
}