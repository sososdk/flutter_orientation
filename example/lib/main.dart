import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import './orientation_helper.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  OrientationHelper.setPreferredOrientations([DeviceOrientation.portraitUp]);
  OrientationHelper.setEnabledSystemUIOverlays(SystemUiOverlay.values);
  runApp(MaterialApp(home: MyApp()));
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  DeviceOrientation _deviceOrientation;

  StreamSubscription<DeviceOrientation> subscription;

  @override
  void initState() {
    super.initState();
    subscription = OrientationHelper.onOrientationChange.listen((value) {
      // If the widget was removed from the tree while the asynchronous platform
      // message was in flight, we want to discard the reply rather than calling
      // setState to update our non-existent appearance.
      if (!mounted) return;

      setState(() {
        _deviceOrientation = value;
      });

      OrientationHelper.forceOrientation(value);
    });
  }

  @override
  void dispose() {
    _dispose();
    super.dispose();
  }

  // https://github.com/flutter/flutter/issues/28134
  void _dispose() {
    subscription?.cancel();
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () {
        _dispose();
        return Future.value(true);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Orientation Example'),
        ),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text(
                  'Running on: ${_deviceOrientation ?? 'Unknown Orientation'}\n'),
              ElevatedButton(
                  child: Text('FullScreen'),
                  onPressed: () {
                    OrientationHelper.setEnabledSystemUIOverlays([]);
                  }),
              ElevatedButton(
                  child: Text('NormalScreen'),
                  onPressed: () {
                    OrientationHelper.setEnabledSystemUIOverlays(
                        SystemUiOverlay.values);
                  }),
              Spacer(),
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: Text(
                    'Flutter is Google’s UI toolkit for building beautiful, natively compiled applications for mobile, web, and desktop from a single codebase.'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
