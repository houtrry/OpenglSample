package com.houtrry.common_map.text

import android.content.Context
import com.houtrry.common_map.data.TextInfo
import com.houtrry.common_map.data.PerformanceStats

/**
 * 文字渲染器接口
 */
interface ITextRenderer {
    /**
     * 初始化渲染器
     */
    fun initialize()
    
    /**
     * 添加单个文字
     * @param textInfo 文字信息
     */
    fun addText(textInfo: TextInfo)
    
    /**
     * 批量添加文字
     * @param textInfos 文字信息列表
     */
    fun addTexts(textInfos: List<TextInfo>)
    
    /**
     * 删除文字
     * @param textId 文字ID
     */
    fun removeText(textId: String)
    
    /**
     * 渲染所有文字
     * @param mvpMatrix MVP变换矩阵
     */
    fun render(mvpMatrix: FloatArray)
    
    /**
     * 清空所有文字
     */
    fun clear()
    
    /**
     * 获取当前文字数量
     */
    fun getTextCount(): Int
    
    /**
     * 获取性能统计
     */
    fun getPerformanceStats(): PerformanceStats
    
    /**
     * 释放资源
     */
    fun release()
}

/**
 * 基础文字渲染器抽象类
 * @param context Android上下文
 */
abstract class BaseTextRenderer(protected val context: Context) : ITextRenderer {
    
    protected val textInfos = mutableMapOf<String, TextInfo>()
    protected var isInitialized = false
    protected var lastRenderTime = 0f
    protected var memoryUsage = 0L
    
    override fun addText(textInfo: TextInfo) {
        if (!isInitialized) {
            initialize()
        }
        textInfos[textInfo.id] = textInfo
        onTextAdded(textInfo)
    }
    
    override fun addTexts(textInfos: List<TextInfo>) {
        if (!isInitialized) {
            initialize()
        }
        textInfos.forEach { textInfo ->
            this.textInfos[textInfo.id] = textInfo
        }
        onTextsAdded(textInfos)
    }
    
    override fun removeText(textId: String) {
        textInfos.remove(textId)?.let { removedText ->
            onTextRemoved(removedText)
        }
    }
    
    override fun clear() {
        textInfos.clear()
        onAllTextsCleared()
    }
    
    override fun getTextCount(): Int = textInfos.size
    
    override fun getPerformanceStats(): PerformanceStats {
        return PerformanceStats(
            renderTime = lastRenderTime,
            memoryUsage = memoryUsage,
            textCount = getTextCount()
        )
    }
    
    /**
     * 子类实现：单个文字添加后的处理
     */
    protected abstract fun onTextAdded(textInfo: TextInfo)
    
    /**
     * 子类实现：批量文字添加后的处理
     */
    protected abstract fun onTextsAdded(textInfos: List<TextInfo>)
    
    /**
     * 子类实现：文字移除后的处理
     */
    protected abstract fun onTextRemoved(textInfo: TextInfo)
    
    /**
     * 子类实现：所有文字清空后的处理
     */
    protected abstract fun onAllTextsCleared()
    
    /**
     * 检查是否包含指定文字
     */
    fun hasText(textId: String): Boolean = textInfos.containsKey(textId)
    
    /**
     * 获取所有文字信息
     */
    fun getAllTexts(): List<TextInfo> = textInfos.values.toList()
} 