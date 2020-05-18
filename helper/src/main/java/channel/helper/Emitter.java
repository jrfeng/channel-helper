package channel.helper;

import java.util.Map;

public interface Emitter {
    void emit(Map<String, Object> data);
}
