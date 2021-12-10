## [redis](https://cloud.tencent.com/developer/tag/10249) ##


### redis设计与实现 ###

1、数据结构： SDS字符串、redisObject(对象)、链表、跳表、压缩表（zipList)
2、存储方式：

### hget和HgetAll 性能对比 ###

1、压缩列表及压缩技术的本质， 底层由列表和哈希字段实现
2、redisDb 维护了16个库（为什么？）， 维护key字典、expire字典


### 过期删除策略 ###
1、 定时删除， timer定时器
2、惰性删除， 获取检测删除 （被动）
3、定期删除（通过限制操作执行时长和频率来减少删除操作对CPU时间影响）


### RDB save、bgSave ###
自动执行BgSave的条件， 900秒内有1次修改
SAVE 900 1
Save 60 100

RDB的原理：fork（子进程）、copy on write
RDB持久化通过保存数据中的键值对来记录数据库状态不同，AOF则是通过保存Redis服务器执行写命令来记录数据库状态，RDB文件是一个经过压缩的二进制文件，对应不同键值对，采用不同方式保存； AOF追加策略 appendfsync:always | everySec | no, 
Aof重写 rewrite, 创建新的AOF，通过读取数据库中的减值对来实现，对原AOF文件不做任何操作， bgrewrite 期间，服务器会维护AOF重写缓冲区，记录新执行写命令，新AOF文件写完后进行追加，使得新旧两个文件保持一致


### redis的模式 ###
1、总从复制， saveOf SYNC请求，命令传播， 新版PSYNC 完整或部分重同步， 从服务器记录复制偏移量，复制积压缓冲区
2、哨兵, Sentinel会以每两秒一次频率，通过命令连接向所有监视服务器发送命令（s_epoch 、 m_epoch), sentinel通过命令发送消息给服务器，其他sentinel通过订阅接受到sentinel发送的_sentinel_:hello信息，并更新
实例结构进行更新, 通过ping判断主观下线，向其他sentinel进行询问，接受足够多已下线判断后，sentinel进行对主服务器执行故障转移操作，下线通知和回应； 
 >* 选举sentinel leader, 进行故障转移（选主服务器的过程， 最后通讯优先级及复制偏移量等）， sentinel发送 slaveof no one命令，等待从服务器角色(role)升级到master
 >* 修改从服务器的复制目标，主从迁移，同时原主服务器设置为从服务器，重启后sentinel发起slaveEoF命令，让他成为从服务器
3、集群（多主节点）
>+ 集群节点握手
 >+ 为什么分为16384个Slot? 握手成功属于下线状态，需要指派槽 cluster addSlots 0 1 2 3 4 ... 5000,所有槽指派完成进入上线状态， 节点之间相互传播指派信息
 clusterState, O(1)级别获取槽对应的节点， 使用命令执行会提示目标节点
     --slots  --> clusterNode*[16384]
         >    --- [0:5000] --> clusterNode[7001]
         >    --- [5001:10000] --> clusterNode[7002]
         >    --- [10000:16383] --> clusterNode[7003]
>+ 查看一个给定键属于哪个槽： Cluster keySlot "message",  slots_to_keys 跳表， 重新分片， 
>+ watch命令通过watched_keys字典减少键，保留监视key对应的客户端， 触发时机为所有对数据库的修改命令
4、事务ACID（原则性、一致性、隔离性、持久性），redis事务不具备rollback的特性
5、二进制位数组（bit array) 
 >   1 ------------8------------
   buf[0][0][0][0][0][0][0][0][0]
   buf[1][0][0][0][0][0][0][0][0]
   buf[.][0][0][0][0][0][0][0][0]
   buf[n][0][0][0][0][0][0][0][0]
 

### redis核心问题 ###

1、数据一致性


参考文献：
Redis设计与实现


### 专题 ###
[bloom Filter](https://juejin.cn/post/6844903982209449991)
[2](https://juejin.cn/post/6844903801908887566)
[1](https://www.wmyskxz.com/2020/03/11/redis-5-yi-ji-shu-ju-guo-lu-he-bu-long-guo-lu-qi/)

 1、解决缓存穿透问题、去重等问题
 2、本身的设置和占用问题、移除及错误率等(其他，hash存储效率只有50%？ )
  2.1: 误判， 解决误判针对该误判元素建立白名单
  2.2：删除困难，没有计数，可采用Counting Bloom Filter

[HyperLogLog](https://www.wmyskxz.com/2020/03/02/reids-4-shen-qi-de-hyperloglog-jie-jue-tong-ji-wen-ti/)
 1、解决大数据基数统计问题？
[GeoHash 地理位置距离排序](https://www.wmyskxz.com/2020/03/12/redis-6-geohash-cha-zhao-fu-jin-de-ren/)
[跳跃表](https://www.wmyskxz.com/2020/02/29/redis-2-tiao-yue-biao/)
 1: 解决zset存储问题
[博主敖丙](https://github.com/AobingJava/JavaFamily) 阿里云

[pub/sub]
 定义是针对字典维护一个客户端集合，在修改某个字段，进行查找并进行推送，离线客户端是无法接受到消息的，可使用消息中间件处理

[延迟队列]
 zset-> zrangebyscore, 获取N秒之前的数据轮训

[分布式锁](https://www.wmyskxz.com/2020/03/01/redis-3/)
 1：GC出现STW，导致A、B同时获取到锁，不局限于Redis、zk、mysql等
 2：红锁解决单机，主从问题（主宕机、从未同步导致）
 3：2.8以后解决了setNx expire非原子性问题

[删除策略 & 内存淘汰策略]
 1. 过期策略： 定时删除（对cpu不友好）、惰性删除、定期（介于两者之间，设置难于界定）; redis过期策略：  惰性删除和定期的组合， redis.conf中hz控制， 每秒运行10次，即100ms左右运行一次
 2. 内存淘汰策略： redis.conf 设置内存 maxmemory,通常为物理内存的3/4， 淘汰策略参数为maxmemory-policy
    1）volatile-lru   利用LRU算法移除设置过过期时间的key (LRU:最近使用 Least Recently Used ) 。
    2）allkeys-lru   利用LRU算法移除任何key （和上一个相比，删除的key包括设置过期时间和不设置过期时间的）。通常使用该方式。
    3）volatile-random 移除设置过过期时间的随机key 。
    4）allkeys-random  无差别的随机移除。
    5）volatile-ttl   移除即将过期的key(minor TTL)
    6）noeviction 不移除任何key，只是返回一个写错误 ，默认选项，一般不会选用。
    
[缓存问题： 缓存穿透、缓存击穿、缓存雪崩](https://www.cnblogs.com/ysocean/p/12452023.html)
[2](https://juejin.cn/post/6844903986475057165)
 >+ 1.1：  缓存雪崩， 缓存大面积失效或redis宕机，导致命中DB， 解决方案：  有效期均匀分布、数据预热（提前加载）、高可用保证）
 >+ 1.2:  缓存穿透，查询不存在的key，导致db问题， 解决方案：业务校验、空结果缓存并设置过期、布隆过滤器(bloom filter), 入口网关做ip限流
 >+ 1.3:  缓存击穿， 热点key失效，导致db积压， 解决方案： 设置热点数据不过期、定时更新、 互斥锁（查询为空，加分布式锁处理）
 
[Redis快的原因]
  内存hash, 查找和操作时间复杂度都是O(1);
  单线程无上下文切换和竞争条件
  IO多路复用模型，非阻塞IO

[缓存一致性]
 1、先删除缓存， 后更新， 为更新已加载旧数据问题
 ![延时双删](https://image-static.segmentfault.com/266/212/2662120757-57b4ff0dd88caec7_fix732)
>+ 更好的解决方案，利用db版本号，通过lua对比，版本号大则更新，反之不更新
 2、更新db，再删缓存，删除失败导致的不一致（最终一致性）
>+ 借助mq, 后删除最终一致
>+ 借助binlog, 消息中间件，针对并发要求不是特别高的 

```text
 针对一致性要求不是很高，可以设置过期时间， 为什么先删后更好， 先更新缓存在更新缓存没有必要
```
[更更 |  删更 | 更删](https://www.cnblogs.com/rjzheng/p/9041659.html)
[1](http://kaito-kidd.com/2021/09/08/how-to-keep-cache-and-consistency-of-db/)
 更更： 加锁更新， mq保证更新失败, 存在线程并发问题更新的
 更删：  mq保证或binlog, 删除失败， A查询B更删除，B删除之前缓存失效A查询旧数据更新，发生概率小，需要解决，可设置有效过期时间，异步延迟删除策略； mq重试引入业务代码入侵，可通过binlog异步串行淘汰， 主从则订阅从库binlog
 删更： 即并发问题， 修改频繁，采用双删，第二次删除还是可能存在失败， 引入mq复杂度变高, 改良后的方案为： 删-> 更 -> 更缓存（lua script version)
![缓存强一致性](https://img-blog.csdnimg.cn/20200910221425525.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTQ0MDExNDE=,size_16,color_FFFFFF,t_70#pic_center)
总结：
>+ 过期兜底， 更新db，在更新缓存
>+ 借助mq解决缓存更新失败的情况
>+ binlog订阅
>+ 通过设置过期时间达到最终一致性 

[CAP理论](https://www.cnblogs.com/crazymakercircle/p/14853622.html)
  C: 一致性， A： 可用性， P： 分区容错性， CA或CP或AP

[redis序列](https://blog.csdn.net/wsdc0521/article/details/107220367)