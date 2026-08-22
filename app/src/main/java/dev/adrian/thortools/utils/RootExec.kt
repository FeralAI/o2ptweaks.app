package dev.adrian.thortools.utils

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.Parcel
import java.nio.charset.Charset

@SuppressLint("DiscouragedPrivateApi", "PrivateApi")
class RootExec {

    private val binder: IBinder?
    var pServerAvailable: Boolean = false
        private set

    init {
        binder = runCatching {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val getService = serviceManager.getDeclaredMethod("getService", String::class.java)
            val binder = getService.invoke(serviceManager, "PServerBinder") as? IBinder
            pServerAvailable = binder?.isBinderAlive == true
            binder
        }.getOrDefault(null)
    }

    fun executeAsRoot(cmd: String): Result<String?> = execute(arrayOf(cmd, "1"))

    fun executeAsRoot(cmd: Array<String>): Result<String?> = execute(cmd + "1")

    private fun execute(arguments: Array<String>): Result<String?> {
        val service = binder ?: return Result.failure(IllegalStateException("PServer not available!"))
        if (!service.isBinderAlive) return Result.failure(IllegalStateException("PServer is not responding"))
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeStringArray(arguments)
            if (!service.transact(0, data, reply, 0)) {
                Result.failure(IllegalStateException("PServer rejected the command"))
            } else {
                val bytes = reply.createByteArray()
                val result = bytes?.toString(Charset.defaultCharset())?.trim()?.let {
                    if (it == "null") null else it
                }
                Result.success(result)
            }
        } catch (error: Throwable) {
            Result.failure(error)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

}
