package com.feralai.o2ptweaks

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
            val binder = getService.invoke(serviceManager, "PServerBinder") as IBinder
            pServerAvailable = true
            binder
        }.getOrDefault(null)
    }

    fun executeAsRoot(cmd: String): Result<String?> {
        if (binder == null) return Result.failure(IllegalStateException("PServer not available!"))

        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        data.writeStringArray(arrayOf(cmd, "1"))
        runCatching { binder!!.transact(0, data, reply, 0) }
            .getOrElse {
                return Result.failure(it)
            }
        val result = reply.createByteArray()?.toString(Charset.defaultCharset())?.trim()?.let {
            if (it == "null") null else it
        }
        data.recycle()
        reply.recycle()
        return Result.success(result)
    }

}