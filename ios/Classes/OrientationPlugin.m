#import "OrientationPlugin.h"
#import <CoreMotion/CoreMotion.h>

const char* const kOrientationUpdateNotificationName = "io.flutter.plugin.platform.SystemChromeOrientationNotificationName";
const char* const kOrientationUpdateNotificationKey = "io.flutter.plugin.platform.SystemChromeOrientationNotificationKey";

@interface OrientationPlugin ()
@property id motionManager;
@property int currentOrientation;
@end

@implementation OrientationPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    OrientationPlugin* instance = [[OrientationPlugin alloc] init];
    
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                     methodChannelWithName:@"sososdk.github.com/orientation"
                                     binaryMessenger:[registrar messenger]];
    [registrar addMethodCallDelegate:instance channel:channel];
    
    FlutterEventChannel* eventChannel = [FlutterEventChannel
                                         eventChannelWithName:@"sososdk.github.com/orientationEvent"
                                         binaryMessenger:[registrar messenger]];
    [eventChannel setStreamHandler:instance];
}

- (id)init {
    self = [super init];
    NSAssert(self, @"super init cannot be nil");
    self.motionManager = [[CMMotionManager alloc] init];
    self.currentOrientation = -1;
    return self;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSString* method = call.method;
    id args = call.arguments;
    if ([method isEqualToString:@"SystemChrome.setPreferredOrientations"]) {
        [self setSystemChromePreferredOrientations:args];
        result(nil);
    } else if ([method isEqualToString:@"SystemChrome.forceOrientation"]) {
        [self forceOrientation:args];
        result(nil);
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)setSystemChromePreferredOrientations:(NSArray*)orientations {
    UIInterfaceOrientationMask mask = 0;
    
    if (orientations.count == 0) {
        mask |= UIInterfaceOrientationMaskAll;
    } else {
        for (NSString* orientation in orientations) {
            if ([orientation isEqualToString:@"DeviceOrientation.portraitUp"])
                mask |= UIInterfaceOrientationMaskPortrait;
            else if ([orientation isEqualToString:@"DeviceOrientation.portraitDown"])
                mask |= UIInterfaceOrientationMaskPortraitUpsideDown;
            else if ([orientation isEqualToString:@"DeviceOrientation.landscapeLeft"])
                mask |= UIInterfaceOrientationMaskLandscapeLeft;
            else if ([orientation isEqualToString:@"DeviceOrientation.landscapeRight"])
                mask |= UIInterfaceOrientationMaskLandscapeRight;
        }
    }
    
    if (!mask)
        return;
    [[NSNotificationCenter defaultCenter] postNotificationName:@(kOrientationUpdateNotificationName)
                                                        object:nil
                                                      userInfo:@{@(kOrientationUpdateNotificationKey) : @(mask)}];
}

- (void)forceOrientation:(NSString*)orientation {
    if ([orientation isEqualToString:@"DeviceOrientation.portraitUp"]) {
        [[UIDevice currentDevice] setValue:@(UIInterfaceOrientationPortrait) forKey:@"orientation"];
    } else if ([orientation isEqualToString:@"DeviceOrientation.portraitDown"]) {
        [[UIDevice currentDevice] setValue:@(UIInterfaceOrientationPortraitUpsideDown) forKey:@"orientation"];
    } else if ([orientation isEqualToString:@"DeviceOrientation.landscapeLeft"]) {
        [[UIDevice currentDevice] setValue:@(UIInterfaceOrientationLandscapeLeft) forKey:@"orientation"];
    } else if ([orientation isEqualToString:@"DeviceOrientation.landscapeRight"]) {
        [[UIDevice currentDevice] setValue:@(UIInterfaceOrientationLandscapeRight) forKey:@"orientation"];
    } else {
        [[UIDevice currentDevice] setValue:@(UIInterfaceOrientationUnknown) forKey:@"orientation"];
    }
}

- (FlutterError*)onListenWithArguments:(id)arguments eventSink:(FlutterEventSink)eventSink {
    [self.motionManager
     startDeviceMotionUpdatesToQueue:[[NSOperationQueue alloc] init]
     withHandler:^(CMDeviceMotion* motion, NSError* error) {
         double gravityX = motion.gravity.x;
         double gravityY = motion.gravity.y;
         int angle = round(atan2(gravityX, gravityY) / M_PI * 180.0 + 180);
         if ((self.currentOrientation == 0 && (angle >= 300 || angle <= 60)) ||
             (self.currentOrientation == 1 && (angle >= 30 && angle <= 150)) ||
             (self.currentOrientation == 2 && (angle >= 120 && angle <= 240)) ||
             (self.currentOrientation == 3 && (angle >= 210 && angle <= 330))) {
         } else {
             self.currentOrientation = (angle + 45) % 360 / 90;
         }
         if (self.currentOrientation == 0) {
             eventSink(@"DeviceOrientation.portraitUp");
         } else if (self.currentOrientation == 1) {
             eventSink(@"DeviceOrientation.landscapeRight");
         } else if (self.currentOrientation == 2) {
             eventSink(@"DeviceOrientation.portraitDown");
         } else if (self.currentOrientation == 3) {
             eventSink(@"DeviceOrientation.landscapeLeft");
         }
     }];
    return nil;
}

- (FlutterError*)onCancelWithArguments:(id)arguments {
    [self.motionManager stopDeviceMotionUpdates];
    return nil;
}
@end
