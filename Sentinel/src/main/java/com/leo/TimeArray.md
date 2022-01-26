## 限流的方式 ##
```text
1）Tomcat 使用 maxThreads来实现限流。
2）Nginx的limit_req_zone和 burst来实现速率限流。
3）Nginx的limit_conn_zone和 limit_conn两个指令控制并发连接的总数。
4）时间窗口算法借助 Redis的有序集合可以实现。
5）漏桶算法可以使用Redis-Cell来实现。
6）令牌算法可以解决Google的guava包来实现。
```
### [Zset通过时间窗口进行限流](https://www.jiagou1216.com/blog/plan/843.html) ###
```java
 /**
     * 限流方法（滑动时间算法/时间窗口算法）
     *
     * @param key      限流标识
     * @param period   限流时间范围（单位：秒）
     * @param maxCount 最大运行访问次数
     * @return 是否限流
     */
    private static boolean isPeriodLimiting(String key, int period, int maxCount) {
        // 当前时间戳
        long nowTs = System.currentTimeMillis();
        // 删除非时间段内的请求数据（清除老访问数据，比如 period=60 时，标识清除 60s 以前的请求记录）
        jedis.zremrangeByScore(key, 0, nowTs - period * 1000);
        // 当前请求次数
        long currCount = jedis.zcard(key);
        if (currCount >= maxCount) {
            // 超过最大请求次数，执行限流
            return false;
        }
        // 未达到最大请求数，正常执行业务,请求记录 +1
        jedis.zadd(key, nowTs, "" + nowTs);
        return true;
    }
```



```text
这种方式有两个缺点：
1）使用 ZSet 存储有每次的访问记录，如果数据量比较大时会占用大量的空间，比如60s允许100W访问时；
2）此代码的执行非原子操作，先判断后增加，中间空隙可穿插其他业务逻辑的执行，有可能导致结果不准确。
```

### [漏桶算法](https://www.jiagou1216.com/blog/plan/844.html) ###
> 漏桶算法解决时间窗口，非均衡处理请求问题
> 漏桶算法的实现步骤是，

```text
1）先声明一个队列用来保存请求，这个队列相当于漏斗，
2）当队列容量满了之后就丢弃（溢出）新来的请求，
3）声明一个线程定期从任务队列中获取一个或多个任务进行处理。
```

Redis-Cell 实现限流的方法也很简单，只需要使用一条指令 cl.throttle 即可，使用示例如下：

```text
> cl.throttle mylimit 15 30 60
1）（integer）0 # 0 表示获取成功，1 表示拒绝
2）（integer）15 # 漏斗容量
3）（integer）14 # 漏斗剩余容量
4）（integer）-1 # 被拒绝之后，多长时间之后再试（单位：秒）-1 表示无需重试
5）（integer）2 # 多久之后漏斗完全空出来
其中 15 为漏斗的容量，30 / 60s 为漏斗的速率。
```

### [令牌算法](https://www.jiagou1216.com/blog/plan/845.html)
```text
令牌算法在令牌桶算法中有一个程序以某种恒定的速度生成令牌，并存入令牌桶中。每个请求必须先获取令牌才能执行，请求如果没有获取到令牌，可以选择等待，也可以放弃执行, 
令牌确实是每 100ms 产生一个，而 acquire() 方法为阻塞等待获取令牌，它可以接收一个 int 类型的参数，用于指定获取令牌的个数。替代方法还有 tryAcquire()，此
方法在没有可用令牌时就会返回 false 这样就不会阻塞等待了。tryAcquire() 方法也可以设置超时时间，未超过最大等待时间会阻塞等待获取令牌，如果超过了最大等待时间
仍然没有可用的令牌就会返回 false。
```
```JAVA
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
```
> 注意： 使用 google guava 实现的令牌算法属于程序级别的单机版限流方案，而上面使用 Redis-Cell 的是分布式的限流方案

> [漏桶和令牌的区别1](https://zhuanlan.zhihu.com/p/165006444)
> [漏桶和令牌的区别2](https://www.cnblogs.com/xuwc/p/9123078.html)

### [RXJAVA](https://www.cnblogs.com/jymblog/p/11731546.html)

### [性能参数](https://www.cnblogs.com/uncleyong/p/11059556.html)
[other](https://www.cnblogs.com/dayu2019/p/11906855.html)
```text
并发量与QPS之间的关系：

QPS = 并发量 / 平均响应时间
并发量 = QPS * 平均响应时间

典型案例：一个OA签到系统，某公司假设有600个人进行上班打卡，8:00为签到时间，
从7:50至8:00这10分钟之内，600个人访问此系统，假设每人访问签到一次为1分钟。
请问：此OA系统的QPS是多少？并发数为多少？

首先确定平均响应时间，平均响应时间 = 1*60 = 60秒
QPS =   600/（10*60）=1 人/秒
并发量  =  QPS  * 平均响应时间 = 1*60 = 60人
```
### [滑动窗口](https://blog.csdn.net/wangdatao_/article/details/107795743)

[固定窗口和滑动窗口](https://blog.csdn.net/weixin_41247920/article/details/100144184)
>+ [other](https://blog.csdn.net/weixin_34273481/article/details/88752687)
>+ #WindowAlg   ---> sentinel#RequestLimiterTest

### [sentinel 原理分析](https://www.cnblogs.com/wuzhenzhao/p/11453649.html) 
https://www.cnblogs.com/mrxiaobai-wen/p/14212637.html

### [信号量] 


