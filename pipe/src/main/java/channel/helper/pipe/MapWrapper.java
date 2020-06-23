package channel.helper.pipe;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("raw parameterized")
final class MapWrapper implements Parcelable {
    private Map<String, Object> mMap;

    MapWrapper(Map<String, Object> map) {
        mMap = map;
    }

    private MapWrapper(Parcel in) {
        mMap = new HashMap<>();

        in.readMap(mMap,Thread.currentThread().getContextClassLoader());
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

    public Map<String, Object> getMap() {
        return mMap;
    }
}
