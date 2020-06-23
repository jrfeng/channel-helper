package channel.helper.pipe;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import channel.helper.Dispatcher;
import channel.helper.Emitter;

public class HandlerPipe extends Handler implements Emitter {
    private static final String TAG = "HandlerPipe";
    private final WeakReference<Dispatcher> mDispatcherWeakReference;

    public HandlerPipe(Dispatcher dispatcher) {
        mDispatcherWeakReference = new WeakReference<>(dispatcher);
    }

    public HandlerPipe(Looper looper, Dispatcher dispatcher) {
        super(looper);
        mDispatcherWeakReference = new WeakReference<>(dispatcher);
    }

    @Override
    public void emit(Map<String, Object> data) {
        Message message = Message.obtain();
        message.obj = data;
        sendMessage(message);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        Dispatcher dispatcher = mDispatcherWeakReference.get();
        if (dispatcher == null) {
            return;
        }

        dispatcher.dispatch(getData(msg));
    }

    @SuppressWarnings("unchecked cast")
    private Map<String, Object> getData(Message dataWrapper) {
        if (dataWrapper.obj == null) {
            Log.d(TAG, "dataWrapper is empty.");
            return new HashMap<>();
        }

        if (!(dataWrapper.obj instanceof Map)) {
            Log.d(TAG, "dataWrapper is empty.");
            return new HashMap<>();
        }

        return (Map<String, Object>) dataWrapper.obj;
    }
}
