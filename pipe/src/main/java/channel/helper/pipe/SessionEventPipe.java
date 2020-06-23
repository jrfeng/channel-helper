package channel.helper.pipe;

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.Map;

import channel.helper.Dispatcher;
import channel.helper.Emitter;

public final class SessionEventPipe implements Emitter {
    private static final String SESSION_EVENT = "channel.helper.pipe.SESSION_EVENT";
    private static final String KET_EXTRA = "extra";

    private MediaSessionCompat mMediaSessionCompat;
    private Dispatcher mDispatcher;

    public SessionEventPipe(MediaSessionCompat mediaSessionCompat) {
        mMediaSessionCompat = mediaSessionCompat;
    }

    public SessionEventPipe(Dispatcher dispatcher) {
        mDispatcher = dispatcher;
    }

    @Override
    public void emit(Map<String, Object> data) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KET_EXTRA, new MapWrapper(data));

        mMediaSessionCompat.sendSessionEvent(SESSION_EVENT, bundle);
    }

    /**
     * Dispatch session event. Invoke at {@link android.support.v4.media.session.MediaControllerCompat.Callback#onSessionEvent(String, Bundle)}
     */
    public boolean dispatch(String action, Bundle data) {
        if (!SESSION_EVENT.equals(action)) {
            return false;
        }

        MapWrapper wrapper = data.getParcelable(KET_EXTRA);
        if (wrapper == null) {
            return false;
        }

        return mDispatcher.dispatch(wrapper.getMap());
    }
}
