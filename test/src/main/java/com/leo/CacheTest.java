package com.leo;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class CacheTest {

    LoadingCache<String, String> cache = CacheBuilder.newBuilder().expireAfterWrite(2,TimeUnit.SECONDS)
                .build(new CacheLoader<String, String>() {
        @Override
        public String load(String key) throws Exception {
            //针对不同的key，同步加载不同的value
            if ("name".equals(key)) {
                try {
                    Logger.info("load:" + key);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "韩信";
            }
            return "";
        }
    });

    static class Logger {
        public static void info(Object value) {
            Thread thread = Thread.currentThread();
            System.out.println(thread + "---" + new Date() + "---" + value);
        }
    }

    @Test
    public void test() throws InterruptedException {
        cache.put("name", "李白");
        //第一次获取缓存值：李白
        Logger.info(cache.getUnchecked("name"));
        Thread.sleep(2000);
        //睡眠2s后，再次获取缓存值，但缓存也失效了，去加载缓存（耗时1s）总共等待的是3s。
        Logger.info(cache.getUnchecked("name"));
        Thread.sleep(1000);
        //睡眠1s后，缓存未失效，再次获取缓存，直接获取到缓存值。
        Logger.info(cache.getUnchecked("name"));
    }

    private static ExecutorService executorService = Executors.newFixedThreadPool(5);


    static LoadingCache<String, String> cache1 = CacheBuilder.
            newBuilder().
            refreshAfterWrite(2, TimeUnit.SECONDS).
            build(new CacheLoader<String, String>() {
                //同步加载缓存
                @Override
                public String load(String key) throws Exception {
                    //针对不同的key，同步加载不同的value
                    if ("name".equals(key)) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return "韩信";
                    }
                    return "";
                }

                //异步加载缓存
                @Override
                public ListenableFuture<String> reload(String key, String oldValue) throws Exception {
                    //定义任务。
                    ListenableFutureTask<String> futureTask = ListenableFutureTask.create(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return "曹操";
                    });
                    //异步执行任务
                    executorService.execute(futureTask);
                    return futureTask;
                }

            });

}
