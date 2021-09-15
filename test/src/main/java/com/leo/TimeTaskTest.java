package com.leo;

import com.sun.corba.se.impl.orbutil.CacheTable;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
