package channel.helper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface Channel {
    @Deprecated
    String name() default "";
    String inspector() default "";
}
