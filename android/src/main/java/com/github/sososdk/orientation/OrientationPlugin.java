package com.github.sososdk.orientation;

import android.app.Activity;
import android.util.Log;
import android.view.OrientationEventListener;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;

/** OrientationPlugin */
public class OrientationPlugin implements MethodCallHandler {
  private final Activity activity;
  private final OrientationEventListener orientationEventListener;
  private EventChannel.EventSink eventSink;

  public OrientationPlugin(Registrar registrar) {
    this.activity = registrar.activity();
    this.orientationEventListener = new OrientationEventListener(activity) {
      @Override public void onOrientationChanged(int angle) {
        if (eventSink != null) {
          try {
            sendOrientationChange(eventSink, convertAngle(angle));
          } catch (RuntimeException e) {
            // Cannot execute operation because FlutterJNI is not attached to native.
            // https://github.com/flutter/flutter/issues/28134
            Log.w("OrientationPlugin", e);
          }
        }
      }
    };
  }

  /** Plugin registration. */
  public static void registerWith(final Registrar registrar) {
    final OrientationPlugin plugin = new OrientationPlugin(registrar);
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.github.sososdk/orientation");
    channel.setMethodCallHandler(plugin);

    EventChannel eventChannel = new EventChannel(registrar.messenger(), "com.github.sososdk/orientationEvent");
    eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
      @Override public void onListen(Object o, EventChannel.EventSink eventSink) {
        plugin.enableOrientationEventListener(eventSink);
      }

      @Override public void onCancel(Object o) {
        plugin.disableOrientationEventListener();
      }
    });
  }

  private void enableOrientationEventListener(EventChannel.EventSink sink) {
    eventSink = sink;
    if (orientationEventListener.canDetectOrientation()) {
      orientationEventListener.enable();
    } else {
      eventSink.error("SensorError", "Cannot detect sensors. Not enabled", null);
    }
  }

  private void disableOrientationEventListener() {
    eventSink = null;
    orientationEventListener.disable();
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    String method = call.method;
    Object arguments = call.arguments;
    try {
      if (method.equals("SystemChrome.setPreferredOrientations")) {
        setSystemChromePreferredOrientations((JSONArray) arguments);
        result.success(null);
      } else if (method.equals("SystemChrome.forceOrientation")) {
        forceOrientation((String) arguments);
        result.success(null);
      } else {
        result.notImplemented();
      }
    } catch (JSONException e) {
      result.error("error", "JSON error: " + e.getMessage(), null);
    }
  }

  private void setSystemChromePreferredOrientations(JSONArray orientations) throws JSONException {
    int requestedOrientation = 0x00;
    for (int index = 0; index < orientations.length(); index += 1) {
      if (orientations.getString(index).equals("DeviceOrientation.portraitUp")) {
        requestedOrientation |= 0x01;
      } else if (orientations.getString(index).equals("DeviceOrientation.landscapeLeft")) {
        requestedOrientation |= 0x02;
      } else if (orientations.getString(index).equals("DeviceOrientation.portraitDown")) {
        requestedOrientation |= 0x04;
      } else if (orientations.getString(index).equals("DeviceOrientation.landscapeRight")) {
        requestedOrientation |= 0x08;
      }
    }
    switch (requestedOrientation) {
      case 0x00:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_UNSPECIFIED);
        break;
      case 0x01:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
        break;
      case 0x02:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
        break;
      case 0x04:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        break;
      case 0x05:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_USER_PORTRAIT);
        break;
      case 0x08:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        break;
      case 0x0a:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_USER_LANDSCAPE);
        break;
      case 0x0b:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_USER);
        break;
      case 0x0f:
        activity.setRequestedOrientation(SCREEN_ORIENTATION_FULL_USER);
        break;
      case 0x03: // portraitUp and landscapeLeft
      case 0x06: // portraitDown and landscapeLeft
      case 0x07: // portraitUp, portraitDown, and landscapeLeft
      case 0x09: // portraitUp and landscapeRight
      case 0x0c: // portraitDown and landscapeRight
      case 0x0d: // portraitUp, portraitDown, and landscapeRight
      case 0x0e: // portraitDown, landscapeLeft, and landscapeRight
        // Android can't describe these cases, so just default to whatever the first
        // specified value was.
        activity.setRequestedOrientation(SCREEN_ORIENTATION_FULL_USER);
        break;
    }
  }

  private void forceOrientation(String orientation) {
    if (orientation.equals("DeviceOrientation.portraitUp")) {
      activity.setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
    } else if (orientation.equals("DeviceOrientation.portraitDown")) {
      activity.setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_PORTRAIT);
    } else if (orientation.equals("DeviceOrientation.landscapeLeft")) {
      activity.setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    } else if (orientation.equals("DeviceOrientation.landscapeRight")) {
      activity.setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
    } else {
      activity.setRequestedOrientation(SCREEN_ORIENTATION_UNSPECIFIED);
    }
  }

  private static void sendOrientationChange(EventChannel.EventSink eventSink, int orientation) {
    String value = orientation(orientation);
    if (value != null) {
      Map<String, Object> event = new HashMap<>();
      event.put("event", "OrientationChange");
      event.put("value", value);
      eventSink.success(event);
    }
  }

  private static String orientation(int orientation) {
    if (orientation == SCREEN_ORIENTATION_PORTRAIT) {
      return "DeviceOrientation.portraitUp";
    } else if (orientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
      return "DeviceOrientation.portraitDown";
    } else if (orientation == SCREEN_ORIENTATION_LANDSCAPE) {
      return "DeviceOrientation.landscapeRight";
    } else if (orientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
      return "DeviceOrientation.landscapeLeft";
    } else {
      return null;
    }
  }

  private static int convertAngle(int angle) {
    int orientation = (angle + 45) % 360 / 90;
    if (orientation == 0) {
      return SCREEN_ORIENTATION_PORTRAIT;
    } else if (orientation == 1) {
      return SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    } else if (orientation == 2) {
      return SCREEN_ORIENTATION_REVERSE_PORTRAIT;
    } else if (orientation == 3) {
      return SCREEN_ORIENTATION_LANDSCAPE;
    } else {
      return SCREEN_ORIENTATION_UNSPECIFIED;
    }
  }
}
