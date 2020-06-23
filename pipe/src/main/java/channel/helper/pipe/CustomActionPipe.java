package channel.helper.pipe;

import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;

import java.util.Map;

import channel.helper.Dispatcher;
import channel.helper.Emitter;

/**
 * Help handle MediaSession custom action easily.
 */
public final class CustomActionPipe implements Emitter {
    private static final String ACTION = "channel.helper.pipe.CUSTOM_ACTION";
    private static final String KEY_DATA = "data";

    private MediaControllerCompat.TransportControls mTransportControls;
    private Dispatcher mDispatcher;

    @Override
    public void emit(Map<String, Object> data) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_DATA, new MapWrapper(data));

        mTransportControls.sendCustomAction(ACTION, bundle);
    }

    /**
     * For {@link MediaControllerCompat.TransportControls#sendCustomAction(String, Bundle)}
     */
    public CustomActionPipe(MediaControllerCompat.TransportControls transportControls) {
        mTransportControls = transportControls;
    }

    /**
     * For {@link android.support.v4.media.session.MediaSessionCompat.Callback#onCustomAction(String, Bundle)}
     */
    public CustomActionPipe(Dispatcher dispatcher) {
        mDispatcher = dispatcher;
    }

    /**
     * Dispatch custom action. Invoke this method at
     * {@link android.support.v4.media.session.MediaSessionCompat.Callback#onCustomAction(String, Bundle)}.
     */
    public boolean dispatch(String action, Bundle data) {
        if (!ACTION.equals(action)) {
            return false;
        }

        MapWrapper wrapper = data.getParcelable(KEY_DATA);
        if (wrapper == null) {
            return false;
        }

        return mDispatcher.dispatch(wrapper.getMap());
    }
}
