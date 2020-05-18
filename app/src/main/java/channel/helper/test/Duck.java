package channel.helper.test;

import channel.helper.Channel;

@Channel
public interface Duck extends Bird {
    void eat();

    void quack(int voice);

    void swing(int speed);
}

// Generated:
//
// package channel.helper.test;
//
//import channel.helper.Pipe;
//
//import java.lang.Object;
//import java.lang.Override;
//import java.lang.String;
//import java.lang.ref.WeakReference;
//import java.util.HashMap;
//import java.util.Map;
//
//public class DuckChannel {
//    private static final String KEY_CLASS_NAME = "__class_name";
//
//    private static final String KEY_METHOD_ID = "__method_id";
//
//    private static final String CLASS_NAME = "channel.helper.test.Duck";
//
//    private static final int METHOD_ID_1 = 1;
//
//    private static final int METHOD_ID_2 = 2;
//
//    private static final int METHOD_ID_3 = 3;
//
//    private static final int METHOD_ID_4 = 4;
//
//    public static class Emitter<T> implements Duck {
//        private Pipe<T> pipe;
//
//        public Emitter(Pipe<T> pipe) {
//            this.pipe = pipe;
//        }
//
//        private void sendMessage(int id, Map<String, Object> args) {
//            args.put(KEY_CLASS_NAME, CLASS_NAME);
//            args.put(KEY_METHOD_ID, id);
//            pipe.emitData(args);
//        }
//
//        @Override
//        public void fly(int high, int speed) {
//            Map<String, Object> args = new HashMap<>();
//            args.put("high", high);
//            args.put("speed", speed);
//            sendMessage(METHOD_ID_1, args);
//        }
//
//        @Override
//        public void eat() {
//            Map<String, Object> args = new HashMap<>();
//            sendMessage(METHOD_ID_2, args);
//        }
//
//        @Override
//        public void quack(int voice) {
//            Map<String, Object> args = new HashMap<>();
//            args.put("voice", voice);
//            sendMessage(METHOD_ID_3, args);
//        }
//
//        @Override
//        public void swing(int speed) {
//            Map<String, Object> args = new HashMap<>();
//            args.put("speed", speed);
//            sendMessage(METHOD_ID_4, args);
//        }
//    }
//
//    public static class Dispatcher<T> {
//        private Pipe<T> pipe;
//
//        private final WeakReference<Duck> callbackWeakReference;
//
//        public Dispatcher(Duck callback) {
//            this.callbackWeakReference = new WeakReference<>(callback);
//        }
//
//        public Dispatcher(Pipe<T> pipe, Duck callback) {
//            this.pipe = pipe;
//            this.callbackWeakReference = new WeakReference<>(callback);
//        }
//
//        public boolean dispatch(Map<String, Object> data) {
//            if (!CLASS_NAME.equals(data.get(KEY_CLASS_NAME))) {
//                return false;
//            }
//            int methodId = (int) data.get(KEY_METHOD_ID);
//            Duck callback = callbackWeakReference.get();
//            if (callback == null) {
//                return false;
//            }
//            switch (methodId) {
//                case METHOD_ID_1:
//                    ;
//                    int METHOD_ID_1_high = (int) data.get("high");
//                    int METHOD_ID_1_speed = (int) data.get("speed");
//                    callback.fly(METHOD_ID_1_high, METHOD_ID_1_speed);
//                    return true;
//                case METHOD_ID_2:
//                    ;
//                    callback.eat();
//                    return true;
//                case METHOD_ID_3:
//                    ;
//                    int METHOD_ID_3_voice = (int) data.get("voice");
//                    callback.quack(METHOD_ID_3_voice);
//                    return true;
//                case METHOD_ID_4:
//                    ;
//                    int METHOD_ID_4_speed = (int) data.get("speed");
//                    callback.swing(METHOD_ID_4_speed);
//                    return true;
//            }
//            return false;
//        }
//    }
//}