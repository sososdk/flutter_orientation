#import "OrientationPlugin.h"
#if __has_include(<orientation/orientation-Swift.h>)
#import <orientation/orientation-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "orientation-Swift.h"
#endif

@implementation OrientationPlugin 
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftOrientationPlugin registerWithRegistrar:registrar];
}
@end
