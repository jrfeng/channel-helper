package channel.helper.pipe;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import channel.helper.Dispatcher;
import channel.helper.Emitter;

/**
 * Messenger Pipe.
 * <p>
 * Supported Param Type:
 * <ul>
 *     <li>null</li>
 *     <li>String</li>
 *     <li>Byte</li>
 *     <li>Short</li>
 *     <li>Integer</li>
 *     <li>Long</li>
 *     <li>Float</li>
 *     <li>Double</li>
 *     <li>Boolean</li>
 *     <li>String[]</li>
 *     <li>boolean[]</li>
 *     <li>byte[]</li>
 *     <li>int[]</li>
 *     <li>long[]</li>
 *     <li>Object[] (supporting objects of the same type defined here).</li>
 *     <li>Bundle</li>
 *     <li>Map (as supported by writeMap(Map)).</li>
 *     <li>Any object that implements the Parcelable protocol.</li>
 *     <li>Parcelable[]</li>
 *     <li>CharSequence (as supported by writeToParcel(CharSequence, Parcel, int)).</li>
 *     <li>List (as supported by writeList(List)).</li>
 *     <li>SparseArray (as supported by writeSparseArray(SparseArray)).</li>
 *     <li>IBinder</li>
 *     <li>Any object that implements Serializable (but see writeSerializable(Serializable) for caveats).
 *     Note that all of the previous types have relatively efficient implementations for writing to
 *     a Parcel; having to rely on the generic serialization approach is much less efficient and
 *     should be avoided whenever possible.</li>
 *
 *     <b>See Method: <a href="https://developer.android.google.cn/reference/android/os/Parcel?hl=en#writeValue(java.lang.Object)">Parcel#writeValue (Object v)</a></b>
 * </ul>
 */
public class MessengerPipe extends Handler implements Emitter {
    private static final String TAG = "MessengerPipe";

    private Messenger mMessenger;
    private Dispatcher mDispatcher;

    public MessengerPipe(IBinder binder) {
        mMessenger = new Messenger(binder);
    }

    /**
     * Use Main Looper
     */
    public MessengerPipe(Dispatcher dispatcher) {
        this(Looper.getMainLooper(), dispatcher);
    }

    public MessengerPipe(Looper looper, Dispatcher dispatcher) {
        super(looper);

        if (dispatcher == null) {
            throw new IllegalArgumentException("param 'dispatcher' is not null.");
        }

        mMessenger = new Messenger(this);
        mDispatcher = dispatcher;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        mDispatcher.dispatch(getData(msg));
    }

    @Override
    public void emit(Map<String, Object> data) {
        Message message = Message.obtain();
        message.obj = new MapWrapper(data);

        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> getData(Message dataWrapper) {
        if (dataWrapper.obj == null) {
            Log.d(TAG, "dataWrapper is empty.");
            return new HashMap<>();
        }

        if (!(dataWrapper.obj instanceof MapWrapper)) {
            Log.d(TAG, "dataWrapper is empty.");
            return new HashMap<>();
        }

        MapWrapper mapWrapper = (MapWrapper) dataWrapper.obj;

        return mapWrapper.getMap();
    }

    public IBinder getBinder() {
        return mMessenger.getBinder();
    }

    public Messenger getMessenger() {
        return mMessenger;
    }

}
