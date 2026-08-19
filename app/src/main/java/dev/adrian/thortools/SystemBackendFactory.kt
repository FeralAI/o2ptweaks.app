package dev.adrian.thortools

import android.content.Context

object SystemBackendFactory {
    fun create(context: Context): SystemBackend {
        if (!BuildConfig.DEBUG) return RealSystemBackend(context)
        return runCatching {
            val factory = Class.forName("dev.adrian.thortools.DebugSystemBackendFactory")
            factory.getDeclaredMethod("create", Context::class.java).invoke(null, context) as SystemBackend
        }.getOrElse { RealSystemBackend(context) }
    }
}
