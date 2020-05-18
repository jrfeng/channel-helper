package channel.helper.pipe;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import channel.helper.Dispatcher;
import channel.helper.Pipe;

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
public class MessengerPipe extends Handler implements Pipe<Message> {
    private static final String TAG = "MessengerPipe";

    private Messenger mMessenger;

    private WeakReference<Dispatcher> mDispatcherWeakReference;
    private boolean mDispatcher;

    public MessengerPipe(IBinder binder) {
        mDispatcher = false;
        mMessenger = new Messenger(binder);
    }

    public MessengerPipe(Dispatcher dispatcher) {
        mDispatcher = true;
        mMessenger = new Messenger(this);
        mDispatcherWeakReference = new WeakReference<>(dispatcher);
    }

    public MessengerPipe(Looper looper, Dispatcher dispatcher) {
        super(looper);
        mDispatcher = true;
        mMessenger = new Messenger(this);
        mDispatcherWeakReference = new WeakReference<>(dispatcher);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        Dispatcher dispatcher = mDispatcherWeakReference.get();
        if (dispatcher == null) {
            return;
        }

        dispatcher.dispatch(getData(msg));
    }

    @Override
    public void emitData(Map<String, Object> data) {
        if (mDispatcher) {
            Log.e(TAG, "The current MessengerPipe can only be a dispatcher.");
            return;
        }

        Message message = Message.obtain();
        message.obj = new MapWrapper(data);

        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getData(Message dataWrapper) {
        if (!mDispatcher) {
            Log.e(TAG, "The current MessengerPipe can only be a emitter.");
            return new HashMap<>();
        }

        if (dataWrapper.obj == null) {
            Log.d(TAG, "dataWrapper is empty.");
            return new HashMap<>();
        }

        if (!(dataWrapper.obj instanceof MapWrapper)) {
            Log.d(TAG, "dataWrapper is empty.");
            return new HashMap<>();
        }

        MapWrapper mapWrapper = (MapWrapper) dataWrapper.obj;

        return (Map<String, Object>) mapWrapper.getMap();
    }

    public IBinder getBinder() {
        if (mDispatcher) {
            return null;
        }

        return mMessenger.getBinder();
    }

    public Messenger getMessenger() {
        return mMessenger;
    }

    private static class MapWrapper implements Parcelable {
        private Map mMap;

        MapWrapper(Map map) {
            mMap = map;
        }

        private MapWrapper(Parcel in) {
            mMap = in.readHashMap(Thread.currentThread().getContextClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeMap(mMap);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<MapWrapper> CREATOR = new Creator<MapWrapper>() {
            @Override
            public MapWrapper createFromParcel(Parcel in) {
                return new MapWrapper(in);
            }

            @Override
            public MapWrapper[] newArray(int size) {
                return new MapWrapper[size];
            }
        };

        public Map getMap() {
            return mMap;
        }
    }
}
