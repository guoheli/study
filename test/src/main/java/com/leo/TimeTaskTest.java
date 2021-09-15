package com.leo;

import com.sun.corba.se.impl.orbutil.CacheTable;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *    Timer---> TimerThread
 *         ---> TaskQueue --> TimeTask[128]
 */
public class TimeTaskTest extends TimerTask {

    public static void main(String[] args) throws InterruptedException {
//        long period = 5 * 1000;
//        Timer timer = new Timer(false);
//        timer.schedule(new TimeTaskTest(), 0, period);
//        Thread.sleep(1000L);

        System.out.println("Start:\t" + new Date());

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            System.out.println("Executor:\t" + new Date());
        }, 60, TimeUnit.SECONDS);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timer:\t" + new Date());
            }
        }, 60000);

    }

    public static class Logger {
        public static void info(String var) {

            System.out.println(Thread.currentThread().getName() + " " + new Date() + "：" + var);
        }
    }

    @Test
    public void test02() throws IOException {
        HashedWheelTimer timer = new HashedWheelTimer();
        Logger.info("start");
        timer.newTimeout(timeout -> {
            Logger.info("1.running");
            Thread.sleep(2000);
            Logger.info("1.end");
        }, 1, TimeUnit.SECONDS);

        timer.newTimeout(timeout -> {
            Logger.info("2.running");
            Thread.sleep(2000);
            Logger.info("2.end");
        }, 1, TimeUnit.SECONDS);

        System.in.read();
    }

    @Override
    public void run() {
        try{
            System.out.println("ok");
            String a=null;
            System.out.println(a.length());
        }catch (Exception e) {

        }

    }
}
