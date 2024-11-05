package com.example.sanadpay_demo

import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.native_print/print"


    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "printImage" -> {
                    val imageData = call.argument<ByteArray>("imageData")
                    if (imageData != null) {
                        doPrint(imageData, result)
                    } else {
                        result.success(false)
                    }
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun doPrint(imageData: ByteArray, result: MethodChannel.Result) {
        val printer = SanadPayPrint(this, result)
        printer.print(imageData)
    }
}
