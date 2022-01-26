package com.leo.algorithm;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;

import java.time.Instant;

/**
 * 固定窗口和滑动窗口
 *
 * @author 80325089
 */
public class WindowAlg {

    Long start = System.currentTimeMillis();

    // 窗口长度是0.1s
    int max = 100;
    Long interval = 1000L;
    int count = 0;

    public static void main(String[] args) {
        // 每秒产生 10 个令牌（每 100 ms 产生一个）
        RateLimiter rt = RateLimiter.create(10);
        for (int i = 0; i < 11; i++) {
            new Thread(() -> {
                // 获取 1 个令牌
                rt.acquire();
                System.out.println("正常执行方法，ts:" + Instant.now());
            }).start();
        }
    }

    @Test
    public boolean trafficMonitoring() {

        long time = System.currentTimeMillis();
        if(time < start + interval) {
            count ++;
            return count > max ? false : true;
        } else {
            count = 1;
            start = time;
            return true;
        }
    }


    int winLength = 100;


    public boolean winTraffic() {


        return false;
    }
}
