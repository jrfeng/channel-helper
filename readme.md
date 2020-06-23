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
    implementation 'com.github.jrfeng.channel-helper:helper:1.2.0'
    implementation 'com.github.jrfeng.channel-helper:pipe:1.2.0'
    annotationProcessor 'com.github.jrfeng.channel-helper:processor:1.2.0'
}
```

## How to use

**Step 1**. Create an interface, example:

**Warning! return type must be `void`.**

```java
public interface Duck {
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
public interface Duck {
    void eat();

    void quack(int voice);

    void swing(int speed);

    void fly(int high, int speed);
}
```

**Step 3**. Use `ChannelHelper` to create emitter and dispatcher.

**`ChannelHelper`：**

```java
public final class ChannelHelper {
    ...
    // Emitter Factory
    public static <T> T newEmitter(Class<T> clazz, Emitter pipe) {...}

    // Dispatcher Factory
    public static <T> Dispatcher newDispatcher(Class<T> clazz, T receiver) {...}
}
```

**Example 1**： Use with `HandlerPipe`

```java
Duck receiver = new Duck() {
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
};

Dispatcher duckDispatcher = ChannelHelper.newDispatcher(Duck.class, receiver);
Duck emitter = ChannelHelper.newEmitter(Duck.class, new HandlerPipe(duckDispatcher));

emitter.eat();          // output: eat
emitter.quack(8);       // output: quack: {voice:8}
emitter.fly(5, 12);     // output: fly: {high:5, speed:12}
emitter.swing(7);       // output: swing: {speed:12}
```

**Example 2**: Use with `MessengerPipe` for `IPC`

Service:

```java
public class TestService extends Service {
    private MessengerPipe mMessengerPipe;
    
    @Override
    public void onCreate() {
        super.onCreate();
        ...
        
        Duck receiver = new Duck() {
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
        };
        
        Dispacher dispatcher = ChannelHelper.newDispatcher(Duck.class, receiver);
        mMessengerPipe = new MessengerPipe(dispatcher);
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
    private Duck mEmitter;
    
    private boolean mConnected;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mMessengerPipe = new MessengerPipe(service);
        mEmitter = ChannelHelper.newEmitter(Duck.class, mMessengerPipe);
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

**Warnning! if you use `MessengerPipe` for `IPC`, Make sure the parameter type of the method meets the requirements of [`Parcel.writeValue(Object)`](https://developer.android.com/reference/android/os/Parcel#writeValue(java.lang.Object)).**

### MediaSession

* `CustomActionPipe`: Help handle custom action.
* `SessionEventPipe`: Help handle session event.

**`CustomActionPipe` Example:**

```java
// Sender
CustomActionPipe pipe = new CustomActionPipe(mTransportControls);
Duck emitter = ChannelHelper.newEmitter(Duck.class, pipe);

emitter.eat();
mEmitter.quack(8);
mEmitter.fly(5, 12);
mEmitter.swing(7);

// MediaSessionCompat.Callback
public class Callback extends MediaSessionCompat.Callback {
    private CustomActionPipe mPipe; 

    public Callback() {
        Duck receiver = new Duck() {
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
        };

        Dispacher dispatcher = ChannelHelper.newDispatcher(Duck.class, receiver);
        mPipe = new CustomActionPipe(dispatcher);
    }

    ...

    @Override
    public void onCustomAction(String action, Bundle extras) {
        mPipe.dispatch(action, extras);
    }
}
```

**`SessionEventPipe` Example:**

```java
// Sender
SessionEventPipe pipe = new SessionEventPipe(mMediaSessionCompat);
Duck emitter = ChannelHelper.newEmitter(Duck.class, pipe);

emitter.eat();
mEmitter.quack(8);
mEmitter.fly(5, 12);
mEmitter.swing(7);

// MediaControllerCompat.Callback
public class Callback extends MediaControllerCompat.Callback {
    private SessionEventPipe mPipe;

    public Callback() {
        Duck receiver = new Duck() {
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
        };

        Dispacher dispatcher = ChannelHelper.newDispatcher(Duck.class, receiver);
        mPipe = new SessionEventPipe(dispatcher);
    }

    ...

    @Override
    public void onSessionEvent(String event, Bundle extras) {
        mPipe.dispatch(event, extras);
    }
}
```

## Other

### Use enum ordinal

You can use the `@UseOrdinal` annotation to label an enumeration type parameter, this will replaces enumeration values with it ordinal integer. This is helpful for `IPC` because `Parcel` does not support enumeration types.

**Example:**

```java
public enum Color {
    REG, GREEN, BLUE
}

public interface Foo {
    void setColor(@UseOrdinal Color color);
}
```

### Merge multiple dispatcher 

We can use static method `DispatcherUtil.merge(Dispacher dispatcher, Dispacher... others)` multiple dispatcher.

**`Example`:**

```java
Duck duckReceiver = new Duck() {/*...*/};
Chicken chickenReceiver = new Chicken() {/*...*/});

// merge multiple dispatcher
Dispatcher mergeDispatcher = DispatcherUtil.merge(
        ChannelHelper.newDispatcher(Duck.class, duckReceiver),
        ChannelHelper.newDispatcher(Chicken.class, chickenReceiver)
    );

// pipe
HandlerPipe handlerPipe = new HandlerPipe(mergeDispatcher);

// create emitter: share handlerPipe
Duck duckEmitter = ChannelHelper.newEmitter(Duck.class, handlerPipe);
Chicken chickenEmitter = ChannelHelper.newEmitter(Chicken.class, handlerPipe);
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