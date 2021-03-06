package channel.helper;

import java.util.Map;

public interface Dispatcher {
    boolean dispatch(Map<String, Object> data);

    boolean match(Map<String, Object> data);
}
