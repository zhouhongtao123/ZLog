package com.jasonzhou.zlog.handler

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.lang.reflect.Field


class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    companion object {
        //CrashHandler实例
        val INSTANCE by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            CrashHandler()
        }
    }

    private val TAG = "CrashHandler"

    //系统默认的UncaughtException处理类
    private lateinit var mDefaultHandler: Thread.UncaughtExceptionHandler


    //程序的Context对象
    private lateinit var mContext: Context

    //用来存储设备信息和异常信息
    private val infos: MutableMap<String, String> = HashMap()


    /**
     * 初始化
     *
     * @param context
     */
    fun init(context: Context): Boolean {
        mContext = context
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler() ?: return false
        //设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
        return true
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex)) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex)
        } else {
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "error : ", e)
            } finally {
            }
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return false
        }
        //使用Toast来显示异常信息
        object : Thread() {
            override fun run() {
                Looper.prepare()
                Toast.makeText(mContext, "很抱歉,程序出现异常,即将退出.", Toast.LENGTH_LONG).show()
                Looper.loop()
            }
        }.start()
        //        收集设备参数信息
//        collectDeviceInfo(mContext);
        //保存日志文件
        saveCrashInfo2File(ex)
        return false
    }

    /**
     * 收集设备参数信息
     * @param ctx
     */
    fun collectDeviceInfo(ctx: Context) {
        try {
            val pm: PackageManager = ctx.packageManager
            val pi: PackageInfo = pm.getPackageInfo(ctx.packageName, PackageManager.GET_ACTIVITIES)
            val versionName = if (pi.versionName == null) "null" else pi.versionName
            val versionCode = pi.versionCode.toString() + ""
            infos["versionName"] = versionName
            infos["versionCode"] = versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "an error occured when collect package info", e)
        }
        //获取手机所有类型信息
        //getDeclaredFields是获取所有申明信息
        val fields: Array<Field> = Build::class.java.declaredFields
        for (field in fields) {
            try {
                field.isAccessible = true
                infos[field.name] = field.get(null).toString()
            } catch (e: Exception) {
                Log.e(TAG, "an error occured when collect crash info", e)
            }
        }
        try {
            //这里获取的是手机imei信息,我写在另一个类里了，参见上篇关于TelephonyManager的博文
//            infos.put("imei", BaseUtils.getInfo(mContext));
            //通过WifiManager获取手机MAC地址 只有手机开启wifi才能获取到mac地址
//            infos.put("mac", BaseUtils.getMacAddress(mContext));
            //这个获取的是手机屏幕信息，在另一个类里，就不po文了
//            infos.put("screen", Constant.ScreenHeight+"*"+Constant.ScreenWith);
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex
     * @return  返回文件名称,便于将文件传送到服务器
     */
    private fun saveCrashInfo2File(ex: Throwable): String? {
        var mobilemsg = ""
        //遍历HashMap
        for ((key, value) in infos) {
            mobilemsg += "$key=$value\r\n"
        }
        val writer: Writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val errormsg: String = writer.toString()
        Log.e(TAG, errormsg)
        return null
    }
}