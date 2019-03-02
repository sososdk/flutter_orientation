import 'dart:async';

import 'package:flutter/services.dart';

class OrientationPlugin {
  static const MethodChannel _channel = const MethodChannel('orientation');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
