package channel.helper;

import java.util.Map;

public interface Pipe<T> {
    void emitData(Map<String, Object> data);

    Map<String, Object> getData(T dataWrapper);
}
