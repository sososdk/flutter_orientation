package com.github.sososdk.orientation

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import android.view.View
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

class MethodCallHandlerImpl : MethodCallHandler {
    companion object {
        private fun sendOrientationChange(eventSink: EventSink, orientation: Int) {
            val value = orientation(orientation)
            if (value != null) {
                eventSink.success(value)
            }
        }

        private fun orientation(orientation: Int): String? {
            return when (orientation) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> "DeviceOrientation.portraitUp"
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT -> "DeviceOrientation.portraitDown"
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> "DeviceOrientation.landscapeRight"
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> "DeviceOrientation.landscapeLeft"
                else -> null
            }
        }
    }

    private var activity: Activity? = null
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var orientationEventListener: OrientationEventListener? = null
    private var currentOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    /**
     * Registers this instance as a method call handler on the given `messenger`.
     *
     *
     * Stops any previously started and unstopped calls.
     *
     *
     * This should be cleaned with [.stopListening] once the messenger is disposed of.
     */
    fun startListening(activity: Activity?, messenger: BinaryMessenger?) {
        this.activity = activity
        channel = MethodChannel(messenger, "sososdk.github.com/orientation")
        channel.setMethodCallHandler(this)
        eventChannel = EventChannel(messenger, "sososdk.github.com/orientationEvent")
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, eventSink: EventSink) {
                val orientationEventListener: OrientationEventListener = object : OrientationEventListener(activity) {
                    override fun onOrientationChanged(angle: Int) {
                        sendOrientationChange(eventSink, convertAngle(angle))
                    }
                }
                if (orientationEventListener.canDetectOrientation()) {
                    orientationEventListener.enable()
                } else {
                    eventSink.error("SensorError", "Cannot detect sensors. Not enabled", null)
                }
            }

            override fun onCancel(o: Any) {
                orientationEventListener?.disable()
                orientationEventListener = null
            }
        })
    }

    /**
     * Clears this instance from listening to method calls.
     *
     *
     * Does nothing if [.startListening] hasn't been called, or if we're already stopped.
     */
    fun stopListening() {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (activity == null) {
            result.error("NO_ACTIVITY", "OrientationPlugin requires a foreground activity.", null)
            return
        }
        val method = call.method
        val arguments = call.arguments
        when (method) {
            "SystemChrome.setEnabledSystemUIOverlays" -> {
                setSystemChromeEnabledSystemUIOverlays(arguments as List<*>)
                result.success(null)
            }
            "SystemChrome.setPreferredOrientations" -> {
                setSystemChromePreferredOrientations(arguments as List<*>)
                result.success(null)
            }
            "SystemChrome.forceOrientation" -> {
                forceOrientation(arguments as String)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private fun setSystemChromeEnabledSystemUIOverlays(overlaysToShow: List<*>) {
        // Start by assuming we want to hide all system overlays (like an immersive game).
        var enabledOverlays = (View.SYSTEM_UI_FLAG_VISIBLE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        // Re-add any desired system overlays.
        for (i in overlaysToShow.indices) {
            when (overlaysToShow[i]) {
                "SystemUiOverlay.top" -> enabledOverlays = enabledOverlays and View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
                "SystemUiOverlay.bottom" -> enabledOverlays = enabledOverlays and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()
            }
        }
        activity!!.window.decorView.systemUiVisibility = enabledOverlays
    }

    private fun setSystemChromePreferredOrientations(orientations: List<*>) {
        var requestedOrientation = 0x00
        var index = 0
        while (index < orientations.size) {
            when (orientations[index]) {
                "DeviceOrientation.portraitUp" -> requestedOrientation = requestedOrientation or 0x01
                "DeviceOrientation.landscapeLeft" -> requestedOrientation = requestedOrientation or 0x02
                "DeviceOrientation.portraitDown" -> requestedOrientation = requestedOrientation or 0x04
                "DeviceOrientation.landscapeRight" -> requestedOrientation = requestedOrientation or 0x08
            }
            index += 1
        }
        when (requestedOrientation) {
            0x00 -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            0x01 -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            0x02 -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            0x04 -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            0x05 -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            0x08 -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            0x0a -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            0x0b -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            0x0f -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            0x03, 0x06, 0x07, 0x09, 0x0c, 0x0d, 0x0e ->         // Android can't describe these cases, so just default to whatever the first
                // specified value was.
                activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
    }

    private fun forceOrientation(orientation: String) {
        when (orientation) {
            "DeviceOrientation.portraitUp" -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "DeviceOrientation.portraitDown" -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "DeviceOrientation.landscapeLeft" -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "DeviceOrientation.landscapeRight" -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun convertAngle(angle: Int): Int {
        if (!(currentOrientation == 0 && (angle >= 300 || angle <= 60) ||
                        currentOrientation == 1 && angle >= 30 && angle <= 150 ||
                        currentOrientation == 2 && angle >= 120 && angle <= 240 ||
                        currentOrientation == 3 && angle >= 210 && angle <= 330)) {
            currentOrientation = (angle + 45) % 360 / 90
        }
        return when (currentOrientation) {
            0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            1 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            2 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            3 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}