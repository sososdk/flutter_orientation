import CoreMotion
import Flutter

let kOrientationUpdateNotificationName = "io.flutter.plugin.platform.SystemChromeOrientationNotificationName"
let kOrientationUpdateNotificationKey = "io.flutter.plugin.platform.SystemChromeOrientationNotificationKey"

public class SwiftOrientationPlugin: NSObject, FlutterPlugin, FlutterStreamHandler{
    private var motionManager: CMMotionManager?
    private var uiOrientationLast: UIInterfaceOrientation
    private var currentOrientation = 0
    
    public class func register(with registrar: FlutterPluginRegistrar) {
        let instance: SwiftOrientationPlugin = SwiftOrientationPlugin()
        
        let channel = FlutterMethodChannel(
            name: "sososdk.github.com/orientation",
            binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let eventChannel = FlutterEventChannel(
            name: "sososdk.github.com/orientationEvent",
            binaryMessenger: registrar.messenger())
        eventChannel.setStreamHandler(instance)
    }
    
    override init() {
        motionManager = CMMotionManager()
        currentOrientation = -1
        motionManager?.accelerometerUpdateInterval = 0.2
        motionManager?.gyroUpdateInterval = 0.2
        uiOrientationLast = .unknown
        
        super.init()
    }
    
    public func handle(_ call: FlutterMethodCall, result: FlutterResult) {
        let method = call.method
        let args = call.arguments
        if method == "SystemChrome.setPreferredOrientations" {
            setSystemChromePreferredOrientations(args as? [AnyHashable])
            result(nil)
        } else if method == "SystemChrome.forceOrientation" {
            forceOrientation(args as? String)
            result(nil)
        } else {
            result(FlutterMethodNotImplemented)
        }
    }
    
    func setSystemChromePreferredOrientations(_ orientations: [AnyHashable]?) {
        var mask = UIInterfaceOrientationMask(rawValue: 0)
        
        if orientations?.count == 0 {
            mask.insert(.all)
        } else {
            for orientation in orientations ?? [] {
                guard let orientation = orientation as? String else {
                    continue
                }
                if orientation == "DeviceOrientation.portraitUp" {
                    mask.insert(.portrait)
                } else if orientation == "DeviceOrientation.portraitDown" {
                    mask.insert(.portraitUpsideDown)
                } else if orientation == "DeviceOrientation.landscapeLeft" {
                    mask.insert(.landscapeLeft)
                } else if orientation == "DeviceOrientation.landscapeRight" {
                    mask.insert(.landscapeRight)
                }
            }
        }
        
        NotificationCenter.default.post(
            name: kOrientationUpdateNotificationName as NSString as NSNotification.Name,
            object: nil,
            userInfo: [
                kOrientationUpdateNotificationKey: mask.rawValue
            ])
    }
    
    func forceOrientation(_ orientation: String?) {
        if orientation == "DeviceOrientation.portraitUp" {
            UIDevice.current.setValue(NSNumber(value: UIInterfaceOrientation.portrait.rawValue), forKey: "orientation")
        } else if orientation == "DeviceOrientation.portraitDown" {
            UIDevice.current.setValue(NSNumber(value: UIInterfaceOrientation.portraitUpsideDown.rawValue), forKey: "orientation")
        } else if orientation == "DeviceOrientation.landscapeLeft" {
            UIDevice.current.setValue(NSNumber(value: UIInterfaceOrientation.landscapeLeft.rawValue), forKey: "orientation")
        } else if orientation == "DeviceOrientation.landscapeRight" {
            UIDevice.current.setValue(NSNumber(value: UIInterfaceOrientation.landscapeRight.rawValue), forKey: "orientation")
        } else {
            UIDevice.current.setValue(NSNumber(value: UIInterfaceOrientation.unknown.rawValue), forKey: "orientation")
        }
    }
    
    public func onListen(withArguments arguments: Any?, eventSink: @escaping FlutterEventSink) -> FlutterError? {
        motionManager?.startAccelerometerUpdates(to: (OperationQueue.current)!, withHandler: {
            (accelerometerData, error) -> Void in
            if error == nil {
                let acceleration: CMAcceleration = accelerometerData!.acceleration
                var orientationNew: UIInterfaceOrientation
                
                if acceleration.x >= 0.75 {
                    orientationNew = .landscapeLeft
                }
                else if acceleration.x <= -0.75 {
                    orientationNew = .landscapeRight
                }
                else if acceleration.y <= -0.75 {
                    orientationNew = .portrait
                }
                else if acceleration.y >= 0.75 {
                    orientationNew = .portraitUpsideDown
                }
                else {
                    // Consider same as last time
                    return
                }
                
                if orientationNew == self.uiOrientationLast {
                    return
                }
                self.uiOrientationLast = orientationNew
                
                switch orientationNew {
                case .portrait:
                    eventSink("DeviceOrientation.portraitUp")
                case .portraitUpsideDown:
                    eventSink("DeviceOrientation.portraitDown")
                case .landscapeRight:
                    eventSink("DeviceOrientation.landscapeRight")
                case .landscapeLeft:
                    eventSink("DeviceOrientation.landscapeLeft")
                default:
                    print("unknown orientation %@", orientationNew.rawValue)
                }
            }
            else {
                print("\(error!)")
            }
        })
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        motionManager?.stopDeviceMotionUpdates()
        return nil
    }
}
