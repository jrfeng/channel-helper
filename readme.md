[**English**](./readme_en.md)

**单向消息分发工具，用于简化 Handler 与 Messenger 的使用，以及简化 MediaSession 的 CustomAction 和 SessionEvent 的使用。**

## 配置项目

**第 1 步**：在你的项目的根目录下的 `build.gradle` 文件中添加以下配置：

```groovy
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

**第 2 步**：添加依赖 [![](https://jitpack.io/v/jrfeng/channel-helper.svg)](https://jitpack.io/#jrfeng/channel-helper)

```groovy
dependencies {
    implementation 'com.github.jrfeng.channel-helper:helper:1.2.7'
    implementation 'com.github.jrfeng.channel-helper:pipe:1.2.7'
    annotationProcessor 'com.github.jrfeng.channel-helper:processor:1.2.7'
}
```

## 开始使用

**第 1 步**：创建一个接口，如下例所示：

**注意：接口中方法的返回值类型必须是 `void`。**

```java
public interface Duck {
    void eat();

    void quack(int voice);

    void swing(int speed);

    void fly(int high, int speed);
}
```

**第 2 步**：使用 `@Channel` 注解标注创建的接口，如下例所示：

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

**第 3 步**：使用 `ChannelHelper` 工具类创建一个 `emitter` 和一个 `dispatcher`。

`ChannelHelper` 的工厂方法：

```java
public final class ChannelHelper {
    ...
    // Emitter 工厂
    public static <T> T newEmitter(Class<T> clazz, Emitter pipe) {...}

    // Dispatcher 工厂
    // 需要持有 receiver 的一个强引用
    public static <T> Dispatcher newDispatcher(Class<T> clazz, T receiver) {...}
}
```

**注意： `channel-helper` 使用弱引用来避免内存泄露，因此需要持有 `receiver` 的一个强引用**

**例 1**：与 `HandlerPipe` 配合使用

```java
// 需要持有 receiver 的一个强引用
private Duck mReceiver = new Duck() {
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

...

Dispatcher duckDispatcher = ChannelHelper.newDispatcher(Duck.class, mReceiver);
Duck emitter = ChannelHelper.newEmitter(Duck.class, new HandlerPipe(duckDispatcher));

emitter.eat();          // 输出: eat
emitter.quack(8);       // 输出: quack: {voice:8}
emitter.fly(5, 12);     // 输出: fly: {high:5, speed:12}
emitter.swing(7);       // 输出: swing: {speed:12}
```

**例 2**：与 `MessengerPipe` 配合使用，用于 IPC

Service：

```java
public class TestService extends Service {
    private MessengerPipe mMessengerPipe;
    private Duck mReceiver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        ...
        
        // 需要持有 receiver 的一个强引用
        mReceiver = new Duck() {
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
        
        Dispacher dispatcher = ChannelHelper.newDispatcher(Duck.class, mReceiver);
        mMessengerPipe = new MessengerPipe(dispatcher);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessengerPipe.getBinder();
    }
}
```

ServiceConnection：

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

**注意：如果你使用 `MessengerPipe` 用于进程间通信，请确保接口中方法的参数类型满足 [`Parcel.writeValue(Object)`](https://developer.android.com/reference/android/os/Parcel#writeValue(java.lang.Object)) 的要求。**

### MediaSession

* **`CustomActionPipe`**：帮助处理 custom action.
* **`SessionEventPipe`**：帮助处理 session event.

**`CustomActionPipe` 示例：**

```java
// 发送端
CustomActionPipe pipe = new CustomActionPipe(mTransportControls);
Duck emitter = ChannelHelper.newEmitter(Duck.class, pipe);

emitter.eat();
emitter.quack(8);
emitter.fly(5, 12);
emitter.swing(7);

// 接收端：MediaSessionCompat.Callback
public class Callback extends MediaSessionCompat.Callback {
    private CustomActionPipe mPipe; 
    private Duck mReceiver;

    public Callback() {
        // 需要持有 receiver 的一个强引用
        mReceiver = new Duck() {
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

        Dispacher dispatcher = ChannelHelper.newDispatcher(Duck.class, mReceiver);
        mPipe = new CustomActionPipe(dispatcher);
    }

    ...

    @Override
    public void onCustomAction(String action, Bundle extras) {
        mPipe.dispatch(action, extras);
    }
}
```

**`SessionEventPipe` 示例：**

```java
// 发送端
SessionEventPipe pipe = new SessionEventPipe(mMediaSessionCompat);
Duck emitter = ChannelHelper.newEmitter(Duck.class, pipe);

emitter.eat();
emitter.quack(8);
emitter.fly(5, 12);
emitter.swing(7);

// 接收端：MediaControllerCompat.Callback
public class Callback extends MediaControllerCompat.Callback {
    private SessionEventPipe mPipe;
    private Duck mReceiver;

    public Callback() {
        // 需要持有 receiver 的一个强引用
        mReceiver = new Duck() {
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

        Dispacher dispatcher = ChannelHelper.newDispatcher(Duck.class, mReceiver);
        mPipe = new SessionEventPipe(dispatcher);
    }

    ...

    @Override
    public void onSessionEvent(String event, Bundle extras) {
        mPipe.dispatch(event, extras);
    }
}
```

## 其他

### 使用枚举值的序数

可以使用 `@UseOrdinal` 注解标记一个枚举类型的参数，这将使用枚举值的序数整数替换枚举值。这对 `IPC` 很有帮助，因为 `Parcel` 不支持枚举类型。

**例：**

```java
public enum Color {
    REG, GREEN, BLUE
}

public interface Foo {
    void setColor(@UseOrdinal Color color);
}
```

### 合并多个 dispatcher 

可以使用静态方法 `DispatcherUtil.merge(Dispacher dispatcher, Dispacher... others)` 合并多个 `dispatcher`。这样的话，你就可以在多个接口之间共享同一个 `pipe` 实例（例如 `HandlerPipe`），而不需要为每个接口创建一个新的 `pipe` 实例。

**例：**

```java
// 需要持有 receiver 的一个强引用
private Duck mDuckReceiver = new Duck() {/*...*/};
private Chicken mChickenReceiver = new Chicken() {/*...*/});

...

// 合并多个 dispatcher
Dispatcher mergeDispatcher = DispatcherUtil.merge(
        ChannelHelper.newDispatcher(Duck.class, mDuckReceiver),
        ChannelHelper.newDispatcher(Chicken.class, mChickenReceiver)
    );

// 创建一个 HandlerPipe 对象
HandlerPipe handlerPipe = new HandlerPipe(mergeDispatcher);

// 创建 emitter：共享同一个 HandlerPipe 对象
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
