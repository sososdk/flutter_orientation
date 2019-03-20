import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class OrientationPlugin {
  static const _methodChannel =
      const MethodChannel('com.github.sososdk/orientation');

  static const _eventChannel =
      const EventChannel('com.github.sososdk/orientationEvent');

  static Future<void> setEnabledSystemUIOverlays(
      List<SystemUiOverlay> overlays) async {
    if (Platform.isAndroid) {
      await _methodChannel.invokeMethod<void>(
        'SystemChrome.setEnabledSystemUIOverlays',
        _stringify(overlays),
      );
    } else {
      SystemChrome.setEnabledSystemUIOverlays(overlays);
    }
  }

  static Future<void> setPreferredOrientations(
      List<DeviceOrientation> orientations) async {
    await _methodChannel.invokeMethod<void>(
      'SystemChrome.setPreferredOrientations',
      _stringify(orientations),
    );
  }

  static Future<void> forceOrientation(DeviceOrientation orientation) async {
    await _methodChannel.invokeMethod<void>(
      'SystemChrome.forceOrientation',
      orientation.toString(),
    );
  }

  static List<String> _stringify(List<dynamic> list) {
    final List<String> result = <String>[];
    for (dynamic item in list) result.add(item.toString());
    return result;
  }

  static Stream<DeviceOrientation> _onOrientationChange;

  static Stream<DeviceOrientation> get onOrientationChange {
    if (_onOrientationChange == null) {
      _onOrientationChange =
          _eventChannel.receiveBroadcastStream().map((event) {
        final Map<dynamic, dynamic> map = event;
        if (map['event'] == 'OrientationChange') {
          return _convert(map['value']);
        }
      });
    }
    return _onOrientationChange;
  }

  static DeviceOrientation _convert(String value) {
    if (value == DeviceOrientation.portraitUp.toString()) {
      return DeviceOrientation.portraitUp;
    } else if (value == DeviceOrientation.portraitDown.toString()) {
      return DeviceOrientation.portraitDown;
    } else if (value == DeviceOrientation.landscapeLeft.toString()) {
      return DeviceOrientation.landscapeLeft;
    } else if (value == DeviceOrientation.landscapeRight.toString()) {
      return DeviceOrientation.landscapeRight;
    } else {
      throw FlutterError('Unknow orientation');
    }
  }
}
