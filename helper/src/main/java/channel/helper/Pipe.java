package channel.helper;

import java.util.Map;

public abstract class Pipe<T> {
    public abstract void emitData(Map<String, Object> data);

    public abstract Map<String, Object> getData(T dataWrapper);
}
