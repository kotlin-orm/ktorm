package me.liuwj.ktorm.logging

import android.util.Log

/**
 * Created by vince on May 05, 2019.
 */
class AndroidLoggerAdapter(val tag: String = "Ktorm") : Logger {

    override fun isTraceEnabled(): Boolean {
        return Log.isLoggable(tag, Log.VERBOSE)
    }

    override fun trace(msg: String, e: Throwable?) {
        Log.v(tag, msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return Log.isLoggable(tag, Log.DEBUG)
    }

    override fun debug(msg: String, e: Throwable?) {
        Log.d(tag, msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return Log.isLoggable(tag, Log.INFO)
    }

    override fun info(msg: String, e: Throwable?) {
        Log.i(tag, msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return Log.isLoggable(tag, Log.WARN)
    }

    override fun warn(msg: String, e: Throwable?) {
        Log.w(tag, msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return Log.isLoggable(tag, Log.ERROR)
    }

    override fun error(msg: String, e: Throwable?) {
        Log.e(tag, msg, e)
    }
}