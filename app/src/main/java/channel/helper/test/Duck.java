package channel.helper.test;

import channel.helper.Channel;

@Channel
public interface Duck extends Bird {
    void eat();

    void quack(int voice);

    void swing(int speed);
}
