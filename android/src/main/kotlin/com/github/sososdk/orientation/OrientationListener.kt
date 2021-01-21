package com.github.sososdk.orientation

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Default OrientationEventListener turn to portraitUp on putting phone on table
 * and turn screen on receiving notifications
 */
abstract class OrientationListener(activity: Activity) : SensorEventListener {
    companion object {
        // Very small values for the accelerometer (on all three axes) should
        // be interpreted as 0. This value is the amount of acceptable
        // non-zero drift.
        const val VALUE_DRIFT = 0.05f
    }

    // System sensor manager instance.
    private val sensorManager: SensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager. If sensor == null then it does not exist
    private val sensorAccelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val sensorMagnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // will fire the first
    private var currentOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    // Current data from accelerometer & magnetometer.  The arrays hold values
    // for X, Y, and Z.
    private var accelerometerData: FloatArray = FloatArray(3)
    private var magnetometerData: FloatArray = FloatArray(3)

    fun enable() {
        // if sensor == null then it is not available
        if (sensorAccelerometer != null) {
            sensorManager.registerListener(this, sensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (sensorMagnetometer != null) {
            sensorManager.registerListener(this, sensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    fun disable() {
        sensorManager.unregisterListener(this);
    }


    override fun onSensorChanged(event: SensorEvent) {
        // The sensorEvent object is reused across calls to onSensorChanged().
        // clone() gets a copy so the data doesn't change out from under us
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> accelerometerData = event.values.clone();
            Sensor.TYPE_MAGNETIC_FIELD -> magnetometerData = event.values.clone();
        }
        // Compute the rotation matrix: merges and translates the data
        // from the accelerometer and magnetometer, in the device coordinate
        // system, into a matrix in the world's coordinate system.
        //
        // The second argument is an inclination matrix, which isn't
        // used in this example.
        val rotationMatrix = FloatArray(9)
        val rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, accelerometerData, magnetometerData);

        // Remap the matrix based on current device/activity rotation.
        var rotationMatrixAdjusted = FloatArray(9)
        rotationMatrixAdjusted = rotationMatrix.clone()
        // following is taken from google tutorial but it goes for rotating all the time
        // https://github.com/google-developer-training/android-advanced/blob/master/TiltSpot/app/src/main/java/com/example/android/tiltspot/MainActivity.java
//        when (display?.rotation) {
//            Surface.ROTATION_0 -> rotationMatrixAdjusted = rotationMatrix.clone()
//            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rotationMatrix,
//                    SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
//                    rotationMatrixAdjusted)
//            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rotationMatrix,
//                    SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
//                    rotationMatrixAdjusted)
//            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rotationMatrix,
//                    SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
//                    rotationMatrixAdjusted)
//        }

        // Get the orientation of the device (azimuth, pitch, roll) based
        // on the rotation matrix. Output units are radians.
        var orientationValues = FloatArray(3)
        if (rotationOK) {
            orientationValues = SensorManager.getOrientation(rotationMatrixAdjusted,
                    orientationValues);
        }

        // Pull out the individual values from the array.
        val azimuth = orientationValues[0]
        var pitch = orientationValues[1]
        var roll = orientationValues[2]

        // Pitch and roll values that are close to but not 0 cause the
        // animation to flash a lot. Adjust pitch and roll to 0 for very
        // small values (as defined by VALUE_DRIFT).
        if (abs(pitch) < VALUE_DRIFT) {
            pitch = 0f
        }
        if (abs(roll) < VALUE_DRIFT) {
            roll = 0f
        }

        val orientationNew = when {
            roll >= 0.75f -> {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            roll <= -0.75 -> {
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            pitch <= -0.75 -> {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            pitch >= 0.75 -> {
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
            else -> currentOrientation
        }

        if (currentOrientation != orientationNew) {
            currentOrientation = orientationNew
            onOrientationChanged(currentOrientation)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /*
     * Returns true if sensor is enabled and false otherwise
     */
    fun canDetectOrientation(): Boolean {
        return sensorAccelerometer != null || sensorMagnetometer != null
    }

    abstract fun onOrientationChanged(orientation: Int)
}