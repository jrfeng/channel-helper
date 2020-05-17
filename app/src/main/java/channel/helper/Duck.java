package channel.helper;

@Channel
public interface Duck {
    void eat();

    void quack(int voice);

    void swing(int speed);

    void fly(int high, int speed);
}
