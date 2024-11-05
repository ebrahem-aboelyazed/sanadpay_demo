package com.example.sanadpay_demo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.morefun.yapi.ServiceResult
import com.morefun.yapi.device.printer.OnPrintListener
import com.morefun.yapi.device.printer.Printer
import com.morefun.yapi.device.printer.PrinterConfig
import com.morefun.yapi.engine.DeviceServiceEngine
import io.flutter.plugin.common.MethodChannel

class SanadPayPrint(private val context: Context, private val result: MethodChannel.Result) {
    private var printer: Printer? = null
    private var printingFinished = false
    private var secoPrinterListener: SecoPrinterListener? = null
    private var deviceServiceEngine: DeviceServiceEngine? = null
    private var isServiceConnected = false

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d("D", "Func: onServiceConnected ::: MoreFunAidlServiceManager")
            deviceServiceEngine = DeviceServiceEngine.Stub.asInterface(service)
            isServiceConnected = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d("D", "Func: onServiceDisconnected ::: MoreFunAidlServiceManager")
            deviceServiceEngine = null
            isServiceConnected = false
        }
    }

    private fun bindService(context: Context) {
        try {
            Log.d("D", "Func: bindService ::: MoreFunAidlServiceManager")
            val serviceAction = "com.morefun.ysdk.service"
            val servicePackage = "com.morefun.ysdk"
            val intent = Intent().apply {
                action = serviceAction
                `package` = servicePackage
            }
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (ex: Exception) {
            Log.e("E", "Error binding service: ${ex.message}")
            ex.printStackTrace()
        }
    }

    fun print(byteArray: ByteArray) {
        try {
            Log.d("D", "Starting print process")
            if (!connectToPrinterService()) return

            if (!setupPrinter()) return

            executePrinting(byteArray)

        } catch (ex: Exception) {
            Log.e("E", "Error during print process: ${ex.message}")
            ex.printStackTrace()
            result.success(false)
        }
    }

    private fun connectToPrinterService(): Boolean {
        if (isServiceConnected) return true

        Log.d("D", "Service not connected, attempting to bind...")
        bindService(context)

        var attempts = 0
        while (!isServiceConnected && attempts < 20) {
            Log.d("D", "Trying to connect, attempt ${attempts + 1}")
            Thread.sleep(500)
            attempts++
        }

        if (!isServiceConnected) {
            Log.e("E", "Failed to connect to printer service")
            result.success(false)
            return false
        }

        return true
    }

    private fun setupPrinter(): Boolean {
        if (printer != null) return true

        printer = deviceServiceEngine?.printer
        if (printer == null) {
            Log.e("E", "Failed to initialize printer")
            result.success(false)
            return false
        }

        try {
            printer?.initPrinter()
            secoPrinterListener = SecoPrinterListener()
            setPrintConfig()
            return true
        } catch (ex: Exception) {
            Log.e("E", "Failed to initialize printer: ${ex.message}")
            result.success(false)
            return false
        }
    }

    private fun executePrinting(byteArray: ByteArray) {
        printer?.also { p ->
            p.initPrinter()
            val status = p.status
            val bitmap = layoutToImage(byteArray)

            if (status == ServiceResult.Success && bitmap != null) {
                printFinalBitmap(bitmap)
                feedPaper()
                result.success(true)
            } else {
                Log.e("E", "Print failed: status=$status, bitmap=${bitmap != null}")
                result.success(false)
            }
        } ?: run {
            Log.e("E", "Printer is null after initialization")
            result.success(false)
        }
    }

    @Throws(RemoteException::class)
    private fun setPrintConfig() {
        Log.d("D", "Func: setPrintConfig SecoPrintServiceImpl ")
        val bundle = Bundle()
        bundle.putInt(PrinterConfig.COMMON_GRAYLEVEL, 30)
        printer?.setConfig(bundle)
    }

    @Throws(Exception::class)
    private fun printFinalBitmap(bitmap: Bitmap?) {
        Log.d("D", "Func: printFinalBitmaps SecoPrintServiceImpl")
        printer?.appendImage(bitmap)
        doPrinting()
        cleanupBitmap(bitmap)
    }

    @Throws(Exception::class)
    private fun doPrinting() {
        Log.d("D", "Func: doPrinting SecoPrintServiceImpl")
        printer?.startPrint(this.secoPrinterListener)
    }

    private fun cleanupBitmap(currentBitmap: Bitmap?) {
        Log.d("D", "Func: cleanupBitmap SecoPrintServiceImpl")
        currentBitmap?.recycle()
    }

    private fun feedPaper() {
        try {
            printer?.appendPrnStr("\n\n\n\n", 4, false)
            this.printingFinished = true
            doPrinting()
        } catch (e: Exception) {
            e.printStackTrace()
            result.success(false)
        }
    }

    private inner class SecoPrinterListener : OnPrintListener.Stub() {
        @Throws(RemoteException::class)
        override fun onPrintResult(retCode: Int) {
            Log.d("D", "onPrintResult retCode ::: $retCode")
            result.success(
                when (retCode) {
                    ServiceResult.Success -> true
                    ServiceResult.Printer_Print_Fail,
                    ServiceResult.Printer_AddPrnStr_Fail,
                    ServiceResult.Printer_AddImg_Fail,
                    ServiceResult.Printer_Busy,
                    ServiceResult.Printer_PaperLack,
                    ServiceResult.Printer_Wrong_Package,
                    ServiceResult.Printer_OutOfMemory,
                    ServiceResult.Printer_No_Printer,
                    ServiceResult.Printer_Low_Power -> false

                    else -> false
                }
            )
        }
    }

    private fun layoutToImage(byteArray: ByteArray): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
        } catch (e: Exception) {
            Log.e("E", "Func: layoutToImage $e")
            null
        }
    }


}