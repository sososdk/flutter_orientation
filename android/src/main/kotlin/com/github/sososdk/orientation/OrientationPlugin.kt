package com.github.sososdk.orientation

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry.Registrar

/** OrientationPlugin  */
class OrientationPlugin : FlutterPlugin, ActivityAware {
    companion object {
        // This static function is optional and equivalent to onAttachedToEngine. It supports the old
        // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
        // plugin registration via this function while apps migrate to use the new Android APIs
        // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
        //
        // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
        // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
        // depending on the user's project. onAttachedToEngine or registerWith must both be defined
        // in the same class.
        fun registerWith(registrar: Registrar) {
            val handler = MethodCallHandlerImpl()
            handler.startListening(registrar.activity(), registrar.messenger())
        }
    }

    private var flutterPluginBinding: FlutterPluginBinding? = null
    private var methodCallHandler: MethodCallHandlerImpl? = null

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        flutterPluginBinding = binding
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        flutterPluginBinding = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        methodCallHandler = MethodCallHandlerImpl()
        methodCallHandler!!.startListening(binding.activity,
                flutterPluginBinding!!.binaryMessenger)
    }

    override fun onDetachedFromActivity() {
        if (methodCallHandler == null) {
            return
        }
        methodCallHandler!!.stopListening()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }
}