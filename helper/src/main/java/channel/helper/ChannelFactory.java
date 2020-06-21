package channel.helper;

import java.lang.reflect.Constructor;

public class ChannelFactory {
    private ChannelFactory() {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked cast")
    public static <T> T newEmitter(Class<T> clazz, Emitter pipe) {
        String emitterName = clazz.getName() + "__ChannelWrapper$Emitter";

        try {
            Class<? extends T> emitter = (Class<? extends T>) Class.forName(emitterName);
            Constructor<? extends T> constructor = emitter.getConstructor(Emitter.class);
            return (T) constructor.newInstance(pipe);
        } catch (Exception e) {
            throw new IllegalStateException("emitter create failed", e);
        }
    }

    @SuppressWarnings("unchecked cast")
    public static <T> Dispatcher newDispatcher(Class<T> clazz, T receiver) {
        String dispatcherName = clazz.getName() + "__ChannelWrapper$Dispatcher";

        try {
            Class<? extends Dispatcher> dispatcher = (Class<? extends Dispatcher>) Class.forName(dispatcherName);
            Constructor<? extends Dispatcher> constructor = dispatcher.getConstructor(clazz);
            return constructor.newInstance(receiver);
        } catch (Exception e) {
            throw new IllegalStateException("dispatcher create failed", e);
        }
    }
}
