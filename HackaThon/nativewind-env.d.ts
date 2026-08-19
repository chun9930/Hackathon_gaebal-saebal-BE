/// <reference types="nativewind/types" />

declare module '*.css';

declare module 'react-native';
declare module 'lucide-react-native';
declare module 'react-native-safe-area-context' {
  export const SafeAreaProvider: any;
  export const SafeAreaView: any;
}

declare module 'react-native-reanimated' {
  const Animated: any;
  export default Animated;
  export const Easing: any;
  export const FadeIn: any;
  export const FadeInUp: any;
  export const useAnimatedStyle: any;
  export const useSharedValue: any;
  export const withTiming: any;
}
