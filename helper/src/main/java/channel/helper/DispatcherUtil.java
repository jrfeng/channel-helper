package channel.helper;

import java.util.Map;

public class DispatcherUtil {
    public static Dispatcher merge(final Dispatcher dispatcher, final Dispatcher... others) {
        return new Dispatcher() {
            @Override
            public boolean dispatch(Map<String, Object> data) {
                if (dispatcher.dispatch(data)) {
                    return true;
                }

                for (Dispatcher d : others) {
                    if (d.dispatch(data)) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean match(Map<String, Object> data) {
                if (dispatcher.match(data)) {
                    return true;
                }

                for (Dispatcher d : others) {
                    if (d.match(data)) {
                        return true;
                    }
                }

                return false;
            }
        };
    }
}
