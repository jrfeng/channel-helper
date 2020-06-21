package channel.helper.test;

import java.util.concurrent.TimeUnit;

import channel.helper.Channel;
import channel.helper.UseOrdinal;

@Channel
public interface Bar extends Foo {
    void noParam();

    void byteParam(byte aByte);

    void shortParam(short aShort);

    void intParam(int aInt);

    void longParam(long aLong);

    void floatParam(float aFloat);

    void doubleParam(double aDouble);

    void stringParam(String aString);

    void enumParam(TimeUnit aEnum1, @UseOrdinal TimeUnit aEnum2);

    void manyParam(byte aByte, short aShort, int aInt, long aLong, float aFloat, double aDouble,
                   String aString, TimeUnit aEnum1, @UseOrdinal TimeUnit aEnum2);

    // DEBUG
    @Channel
    interface Inner {
        void test();
    }
}