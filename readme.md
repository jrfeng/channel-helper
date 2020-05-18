## Download

**Step 1**. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2**. Add the dependency [![](https://jitpack.io/v/jrfeng/channel-helper.svg)](https://jitpack.io/#jrfeng/channel-helper)

```groovy
dependencies {
    implementation 'com.github.jrfeng.channel-helper:helper:1.0.2'
    implementation 'com.github.jrfeng.channel-helper:pipe:1.0.2'
    annotationProcessor 'com.github.jrfeng.channel-helper:processor:1.0.2'
}
```

## How to use

**Step 1**. Create an interface, example:

```java
public interface Duck extends Bird {
    void eat();

    void quack(int voice);

    void swing(int speed);

    void fly(int high, int speed);
}
```

**Step 2**. Annotated with the **`@Channel`** annotation, example:

```java
import channel.helper.Channel;

@Channel
public interface Duck extends Bird {
    void eat();

    void quack(int voice);

    void swing(int speed);

    void fly(int high, int speed);
}
```

**Step 3**. Build your project. Then, annotation processor will generate a helper class, example:

```java
public class DuckChannel {
    private static final String KEY_CLASS_NAME = "__class_name";

    private static final String KEY_METHOD_ID = "__method_id";

    private static final String CLASS_NAME = "channel.helper.test.Duck";

    private static final int METHOD_ID_1 = 1;

    private static final int METHOD_ID_2 = 2;

    private static final int METHOD_ID_3 = 3;

    private static final int METHOD_ID_4 = 4;

    private DuckChannel() {
    }

    public static class Emitter implements Duck {
        private channel.helper.Emitter emitter;

        public Emitter(channel.helper.Emitter emitter) {
            this.emitter = emitter;
        }

        private void sendMessage(int id, Map<String, Object> args) {
            args.put(KEY_CLASS_NAME, CLASS_NAME);
            args.put(KEY_METHOD_ID, id);
            emitter.emit(args);
        }

        @Override
        public void fly(int high, int speed) {
            Map<String, Object> args = new HashMap<>();
            args.put("high", high);
            args.put("speed", speed);
            sendMessage(METHOD_ID_1, args);
        }

        @Override
        public void eat() {
            Map<String, Object> args = new HashMap<>();
            sendMessage(METHOD_ID_2, args);
        }

        @Override
        public void quack(int voice) {
            Map<String, Object> args = new HashMap<>();
            args.put("voice", voice);
            sendMessage(METHOD_ID_3, args);
        }

        @Override
        public void swing(int speed) {
            Map<String, Object> args = new HashMap<>();
            args.put("speed", speed);
            sendMessage(METHOD_ID_4, args);
        }
    }

    public static class Dispatcher implements channel.helper.Dispatcher {
        private final WeakReference<Duck> callbackWeakReference;

        public Dispatcher(Duck callback) {
            this.callbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        public boolean dispatch(Map<String, Object> data) {
            if (!CLASS_NAME.equals(data.get(KEY_CLASS_NAME))) {
                return false;
            }
            int methodId = (int) data.get(KEY_METHOD_ID);
            Duck callback = callbackWeakReference.get();
            if (callback == null) {
                return false;
            }
            switch (methodId) {
                case METHOD_ID_1:
                    int METHOD_ID_1_high = (int) data.get("high");
                    int METHOD_ID_1_speed = (int) data.get("speed");
                    callback.fly(METHOD_ID_1_high, METHOD_ID_1_speed);
                    return true;
                case METHOD_ID_2:
                    callback.eat();
                    return true;
                case METHOD_ID_3:
                    int METHOD_ID_3_voice = (int) data.get("voice");
                    callback.quack(METHOD_ID_3_voice);
                    return true;
                case METHOD_ID_4:
                    int METHOD_ID_4_speed = (int) data.get("speed");
                    callback.swing(METHOD_ID_4_speed);
                    return true;
            }
            return false;
        }
    }
}
```

The name of generated helper class is base on your interface. 

**Format:**

```text
<interface_name>Channel
```

As shown in the example, Because the name of interface is `Duck`, so the name of generated helper 
class is `DuckChannel`.

Of course, you also can custom the name of generate helper class. example:

```java
@Channel(name="CustomName")
public interface Duck extends Bird {
    // ...
}
```

Then, then name of generated helper class will be `CustomName`.

### Use generated helper class

The generated helper class can be used with `HandlerPipe` and `MessengerPipe`, can help you use 
Handler and Messenger easily(don't worry about memory leaks).

**Example 1**: Use with `HandlerPipe`

```java
// Use with HandlerPipe
DuckChannel.Dispatcher dispatcher = new DuckChannel.Dispatcher<>(new Duck() {
    @Override
    public void eat() {
        Log.d("App", "eat");
    }

    @Override
    public void quack(int voice) {
        Log.d("App", "quack: {voice:" + voice + "}");
    }

    @Override
    public void swing(int speed) {
        Log.d("App", "swing: {speed:" + speed + "}");
    }

    @Override
    public void fly(int high, int speed) {
        Log.d("App", "fly: {high:" + high + ", speed:" + speed + "}");
    }
});

HandlerPipe handlerPipe = new HandlerPipe(dispatcher);
DuckChannel.Emitter emitter = new DuckChannel.Emitter(handlerPipe);

emitter.eat();          // output: eat
emitter.quack(8);       // output: quack: {voice:8}
emitter.fly(5, 12);     // output: fly: {high:5, speed:12}
emitter.swing(7);       // output: swing: {speed:12}
```

**Example 2**: Use with `MessengerPipe`

Service:

```java
public class TestService extends Service {
    private MessengerPipe mMessengerPipe;
    private DuckChannel.Dispatcher mDispatcher;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mDispatcher = new DuckChannel.Dispatcher(new Duck() {
            @Override
            public void eat() {
                // ...
            }

            @Override
            public void quack(int voice) {
                // ...
            }

            @Override
            public void swing(int speed) {
                // ...
            }

            @Override
            public void fly(int high, int speed) {
                // ...
            }
        });
        
        mMessengerPipe = new MessengerPipe(mDispatcher);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessengerPipe.getBinder();
    }
}
```

ServiceConnection:

```java
public class TestServiceConnection implements ServiceConnection {
    private MessengerPipe mMessengerPipe;
    private DuckChannel.Emitter mEmitter;
    
    private boolean mConnected;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mMessengerPipe = new MessengerPipe(service);
        mEmitter = new DuckChannel.Emitter(mMessengerPipe);
        mConnected = true;
        
        test();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // ...
    }
    
    private void test() {
        if (!mConnected) {
            return;
        }
        
        mEmitter.eat();
        mEmitter.quack(8);
        mEmitter.fly(5, 12);
        mEmitter.swing(7);
    }
}
```

## Merge multiple dispatcher 

We can use static method `DispatcherUtil.merge(Dispacher dispatcher, Dispacher... others)` multiple dispatcher.

**`Example`:**

```java
DuckChannel.Dispatcher duckDispatcher = new DuckChannel.Dispatcher(new Duck() {/*...*/});
ChickenChannel.Dispatcher chickenDispatcher = new ChickenChannel.Dispatcher(new Chicken() {/*...*/});

// merge multiple dispatcher
Dispatcher mergeDispatcher = DispatcherUtil.merge(duckDispatcher, chickenDispatcher);

// pipe
HandlerPipe handlerPipe = new HandlerPipe(mergeDispatcher);

// emitter: share handlerPipe
DuckChannel.Emitter duckEmitter = new DuckChannel.Emitter(handlerPipe/*share handlerPipe*/);
ChickenChannel.Emitter chickenEmitter = new ChickenChannel.Emitter(handlerPipe/*share handlerPipe*/);
```

## LICENSE

```text
MIT License

Copyright (c) 2020 jrfeng

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```