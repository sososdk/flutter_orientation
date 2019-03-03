import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:orientation/orientation.dart';

// https://github.com/flutter/flutter/issues/28134
void main() => runApp(MaterialApp(
      home: MyApp(),
    ));

class MyApp extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _MyAppState();
  }
}

class _MyAppState extends State<MyApp> {
  DeviceOrientation _deviceOrientation;

  StreamSubscription<DeviceOrientation> subscription;

  @override
  void initState() {
    super.initState();
    subscription = OrientationPlugin.onOrientationChange.listen((value) {
      // If the widget was removed from the tree while the asynchronous platform
      // message was in flight, we want to discard the reply rather than calling
      // setState to update our non-existent appearance.
      if (!mounted) return;

      setState(() {
        _deviceOrientation = value;
      });

      OrientationPlugin.forceOrientation(value);
    });
  }

  @override
  void dispose() {
    subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Orientation'),
      ),
      body: Center(
        child: Text(
            'Running on: ${_deviceOrientation ?? 'Unknown Orientation'}\n'),
      ),
    );
  }
}
