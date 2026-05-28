package com.github.kr328.clash.service.myfeature

import android.util.Log

/**
 * 自定义 Logger 单例对象，提供统一的 TAG 和自动获取调用位置的功能。
 */
object AppLogger {

    /**
     * 指定一个全局唯一的 TAG，用于在 Logcat 中一次性过滤出所有相关日志。
     * 你可以根据你的项目名称进行修改。
     */
    private const val TAG = "ClashAppLogger"

    /**
     *  在调用堆栈中，调用我们 Logger 的那个方法所在的元素的索引。
     *  [0] = Thread.getStackTrace()
     *  [1] = AppLogger.getCallerInfo()
     *  [2] = AppLogger.d() 或 i(), w(), e()
     *  [3] = 真正调用 AppLogger.d() 的那个方法  <-- 这是我们想要的
     */
    private const val CALLER_STACK_INDEX = 3

    /**
     * 一个私有辅助函数，用于获取调用者的类名和方法名。
     * @return 格式化的字符串，如 "[NewProfileActivity.main]"
     */
    private fun getCallerInfo(): String {
        val stackTrace = Thread.currentThread().stackTrace
        // 防止堆栈深度不够导致数组越界
        if (stackTrace.size <= CALLER_STACK_INDEX) {
            return "[Unknown Location]"
        }
        val element = stackTrace[CALLER_STACK_INDEX]

        // 从完整的类名 (com.github.kr328.clash.NewProfileActivity) 中提取出简单的类名
        val className = element.className.substringAfterLast('.')
        val methodName = element.methodName

        return "[$className.$methodName]"
    }

    /**
     * 输出 Debug 级别的日志。
     * @param message 你想输出的自定义信息。
     */
    fun d(message: String) {
        // 将调用位置信息和你的自定义信息拼接起来，一起输出
        Log.d(TAG, "${getCallerInfo()} $message")
    }

    /**
     * 输出 Info 级别的日志。
     * @param message 你想输出的自定义信息。
     */
    fun i(message: String) {
        Log.i(TAG, "${getCallerInfo()} $message")
    }

    /**
     * 输出 Warning 级别的日志。
     * @param message 你想输出的自定义信息。
     * @param throwable (可选) 异常对象。
     */
    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, "${getCallerInfo()} $message", throwable)
    }

    /**
     * 输出 Error 级别的日志。
     * @param message 你想输出的自定义信息。
     * @param throwable (可选) 异常对象。
     */
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, "${getCallerInfo()} $message", throwable)
    }
}