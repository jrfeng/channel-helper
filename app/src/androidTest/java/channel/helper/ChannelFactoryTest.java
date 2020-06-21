package channel.helper;

import android.os.HandlerThread;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import channel.helper.pipe.HandlerPipe;
import channel.helper.test.Bar;

@RunWith(AndroidJUnit4.class)
public class ChannelFactoryTest {
    @Test(timeout = 3000)
    public void factoryTest() throws InterruptedException {
        final String value = "Hello";

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Bar receiver = new Bar() {
            @Override
            public void noParam() {

            }

            @Override
            public void byteParam(byte aByte) {

            }

            @Override
            public void shortParam(short aShort) {

            }

            @Override
            public void intParam(int aInt) {

            }

            @Override
            public void longParam(long aLong) {

            }

            @Override
            public void floatParam(float aFloat) {

            }

            @Override
            public void doubleParam(double aDouble) {

            }

            @Override
            public void stringParam(String aString) {
                assertEquals(value, aString);
                countDownLatch.countDown();
            }

            @Override
            public void enumParam(TimeUnit aEnum1, TimeUnit aEnum2) {

            }

            @Override
            public void manyParam(byte aByte, short aShort, int aInt, long aLong, float aFloat, double aDouble, String aString, TimeUnit aEnum1, TimeUnit aEnum2) {

            }

            @Override
            public void extendsTest(String value) {

            }
        };

        HandlerThread handlerThread = new HandlerThread("ChannelFactoryTest");
        handlerThread.start();

        Dispatcher barDispatcher = ChannelFactory.newDispatcher(Bar.class, receiver);
        Bar emitter = ChannelFactory.newEmitter(Bar.class, new HandlerPipe(handlerThread.getLooper(), barDispatcher));

        emitter.stringParam(value);

        countDownLatch.await();
        handlerThread.quit();
    }
}
