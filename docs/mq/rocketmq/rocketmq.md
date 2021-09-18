### [消息发送](https://blog.csdn.net/pingfanderen123/article/details/105538572) ###

### [源码分析rocketmq](https://blog.csdn.net/prestigeding/article/details/78888290) ###
https://www.cmsblogs.com/?p=5784
https://inetyoung.blog.csdn.net/article/details/111147264
http://www.java1234.com/a/javabook/javabase/2021/0307/19289.html
https://segmentfault.com/blog/dingw_rocketmq


https://zhuanlan.zhihu.com/p/59516998

http://www.itjcw123.cn/6432.html

http://okgoes.cn/blog/detail?blog_id=28950
https://www.iocoder.cn/categories/RocketMQ/
https://www.jhonrain.org/tags/%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90/

https://my.oschina.net/javamaster/blog/2051910
https://www.jianshu.com/p/6b833d01b249
https://blog.csdn.net/csdnnews/article/details/81124883

https://www.jianshu.com/nb/40388944

https://baijiahao.baidu.com/s?id=1638994745278332160&wfr=spider&for=pc


https://blog.51cto.com/u_15015181/2556351

push、pull: https://blog.csdn.net/u014362882/article/details/80424527


###<p> ###

###<p> ###

###<p> ###

###<p> ###

###<p> ###

###<p> ###

###<p> ###

###<p> ###

###<p> ###

###<p> ###

### ==========================黄金分割=================== ###



https://objcoding.com/category/#RocketMQ



### rocketmq 消费处理 ###

> * PullMessageService 阻塞获取 PullRequest, 拉取消息后放回保证了从服务端拉取的有序性; 
> * DefaultMQPushConsumerImpl#pullMessage, 拉取传递 queue的nextOff及commit当前的commitOffsetValue，相当于其他MQ的ACK机制
> * 拉取到消息回调处理流程： pullRequest更新下一次取拉取的nextOffset, 异步提交任务回调业务去处理，重新将pullRequest放入任务，同一个
> 队列出现多线程消费的情况；
> * commitOffsetValue存在在RemoteBrokerOffsetStore中，当业务回调任务完成后，移除已处理的MessageExt, 更新OffsetStore的commitOffSet

##### ProcessQueue #####

>* 存储当前客户端拉取到的最大Offset即queueOffsetMax, 当移除已ack的消息，如果没有剩余则，可用任务已提交offset为queueOffsetMax +1, 反之取
> 第一条的offset值作为commit offset 提交到服务broker节点

```text
【1：1】-【1：2】-【1：3】 -- 【2：4】-【2：5】-【2：6】
                  |
                  V
【1：1】-【1：2】-【1：3】  那么当前commit offset为1，如果出现宕机则全部重新消费
```



##### 顺序和非顺序的区别 #####
>* 顺序消费通过抢到锁，然后通过ProcessQueue的MsgTreeMap顺序获取待消费的值，即ProcessQueue#takeMessages
> 

### 延迟消费问题 ###
>+ [深度博主](https://blog.csdn.net/yuanshangshenghuo/article/details/110913277)
>+ <font color = 'red'> 这里有点要注意的是，你消息消费失败重试也是要走延迟消息的，这块你需要注意下，然后别改变里延迟消息规则，然后造成失败重试有问题，默认失败重试规则就是3+失败重试次数的延时等级 </font>

>+  NettyRemoting --> NettyRemotingAbstract#processMessageReceived --->
> NettyRequestProcessor(SendMessageProcessor) --> DefaultMessageStore#putMessage-->CommitLog#putMessage
> ---> MappedFile#appendMessagesInner-->CommitLog#doAppend --> CommitLog#submitFlushRequest ---> CommitLog#submitReplicaRequest
> 
> 
> 所有的延迟消息放到SCHEDULE_TOPIC_XXXX(topic主题)， delayTimeLevel作为queueId
> CommitLog维护了 topicQueue->queueOffset
> ScheduleMessageService定时轮询，事件到了后提交写commitLog#putMessage进行保证， 这个方法首先是获取对应延迟等级的
> consumeQueue这个队列，取出offset往后的消息，进行遍历，找出每个unit对应的commitlog真实offset，然后通过
> commitlog offset 从commitlog获取到真实的那个消息，根据它的存储实现与延迟时间，算出与真实交付时间的差值，如果是小于等
> 于0的话，说明延迟时间到了，这个时候就要暴露给消息消费者了，它就会将topic与queueId转成之前的那个topic queueId，然后重
> 新扔到commitlog中，这个时候通过reput线程的dispatch处理，消息消费者就能发现这个消息并消费，如果这个差值还不够的话，重新
> 创建调度任务，然后延迟执行时间是这个差值，扔到timer中
> 
> MappedFile
#### 事务 #####

>+ 使用场景
```text
假设我们在做一个电商项目， 然后当用户下单并支付成功的时候，会根据用户支付的金额来计算并向用户放一定的积分，我们这个发放积分动
作不能拖累我们的支付动作，不能因为这种枝叶业务影响我们支付主业务的体验， 所以我们将发放积分的需求使用RocketMQ的事务消息来
完成
```
>+ 原理
```text
（1）我们发送这个事务消息的时候一开始并不是直接发送到你指定的那个topic 对应的队列里面的，而是将消息发送到RMQ_SYS_TRANS_HALF_TOPIC topic里面，然后它返回响应 告诉生产者，执行executeLocalTransaction 方法来执行本地事务，为啥不放了你设置的那个 topic里？就是防止消费者给消费了，这个时候还不知道你本地事务执行情况，就给消费了岂不是很尴尬。
（2）当你本地事务执行成功，返回 commit提交事务，这个时候broker 会 先从RMQ_SYS_TRANS_HALF_TOPIC topic里面找到你那个消息，恢复原来的样子，然后进行存储，这个时候存储就存储到了 你设置到的那个topic里面了。存储成功之后将 形成一个删除消息 然后放到RMQ_SYS_TRANS_OP_HALF_TOPIC topic 里面。放到你原来那个topic里面，你的消费者就可以消费了
（3）如果你本地事务失败，然后就要rollback 了，这个时候先从RMQ_SYS_TRANS_HALF_TOPIC topic里面找到你那个消息，然后形成一个删除消息 然后放到RMQ_SYS_TRANS_OP_HALF_TOPIC topic 里面。
（4）出现网络问题或者服务挂了怎么办？
如果你在发送消息的时候出现了问题，消息是使用同步发送，然后会重试，然后会抛出异常，发送失败，你的本地事务也就不用执行了。
如果你告诉broker 提交事务或者回滚事务的时候出现了问题怎么办？这时候broker 会有个事务服务线程，隔一段时间就扫描RMQ_SYS_TRANS_HALF_TOPIC topic 里面没有提交或者回滚的消息，然后它就会发送消息到你的生产者端来，然后执行checkLocalTransaction 这个检查事务的方法，询问你事务的执行状态。它默认会问你15次，15次没结果就直接删了，估计是绝望了
```

>+ 二阶段事务
> 当支付宝账户扣除1万后，我们只要生成一个凭证（消息）即可，这个凭证（消息）上写着“让余额宝账户增加1万”，只要这个凭证（消息）能可靠保存，
> 我们最终是可以拿着这个凭证（消息）让余额宝账户增加1万的，即我们能依靠这个凭证（消息）完成最终一致性。仍然需要解决[消息重复投递的问题](https://blog.csdn.net/fenglibing/article/details/92417739)
> ![d](https://img-blog.csdnimg.cn/20190616222506954.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2ZlbmdsaWJpbmc=,size_16,color_FFFFFF,t_70)

> 大事务=小事务+异步
![d](https://img-blog.csdnimg.cn/20190616222550408.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2ZlbmdsaWJpbmc=,size_16,color_FFFFFF,t_70)
![d](https://img-blog.csdnimg.cn/20190616222635851.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2ZlbmdsaWJpbmc=,size_16,color_FFFFFF,t_70)
```text
RocketMQ第一阶段发送Prepared消息时，会拿到消息的地址，第二阶段执行本地事物，第三阶段通过第一阶段拿到的地址去访问消息，并修改消息的状态。

细心的你可能又发现问题了，如果确认消息发送失败了怎么办？RocketMQ会定期扫描消息集群中的事物消息，如果发现了Prepared消息，它会向消息发送端(生产者)确认，Bob的钱到底是减了还是没减呢？如果减了是回滚还是继续发送确认消息呢？

RocketMQ会根据发送端设置的策略来决定是回滚还是继续发送确认消息。这样就保证了消息发送与本地事务同时成功或同时失败
```
#### Broker存储篇 ####

![存储](https://img-blog.csdnimg.cn/20201108101621378.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3l1YW5zaGFuZ3NoZW5naHVv,size_16,color_FFFFFF,t_70#pic_center)


##### commitLog 写入流程 #####
> 1. SendMessageProcessor#processRequest --> DefaultMessageStore#putMessage --> MappedFile#appendMessagesInner --> CommitLog#doAppend
> --> CommitLog#handleDiskFlush

> 在commitlog写入消息前会获取一个锁来保证顺序写入，获取到锁就会更新下这个获取锁的时间，最后写入完成会释放锁，将这个时间设置成0 ，如果按照正常情况的话，
> 如果你现在这个时间戳减去0的话绝对会大于这个10000000的，也就是不满足，然后当前时间减去那个上次获取锁的时间，如果现在与获取锁时间差大于1s的话就说明某个
> 追加写入已经持有锁超过1s了，所以它会认为os page cache繁忙

[pageCache](https://img-blog.csdnimg.cn/20201108144630156.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3l1YW5zaGFuZ3NoZW5naHVv,size_16,color_FFFFFF,t_70#pic_center)

##### Reput流程 #####

>+ DefaultMessageStore#start ---> ReputMessageService#doReput-->CommitLog#getData --->获取到进行分发DefaultMessageStore.this.doDispatch(dispatchRequest)
>+ 1.---> CommitLogDispatcherBuildConsumeQueue#dispatcher ->putMessagePositionInfoWrapper
>+ 2.---> CommitLogDispatcherBuildIndex (IndexService 索引主要用于后台查询 topic和Keys进行消息定位使用)
> 
```text

我们知道当消息生产者send一条消息给broker ，broker 先是会将消息存入commitlog中，并将消息存入结果返回给消息生产者， 然后后台有一个reput的线程，不断的
从commitlog中取出消息来，交给不同的dispatcher来进行处理，其中有BuildConsumeQueue这么一个dispatcher，拿到消息的信息后，按照消息不同topic 不同的
queue 找到对应的ConsumeQueue，然后将消息的在commitlog中的一个offset，消息的大小，消息tagcode 根据queue offset 写到consumeQueue对应的位置中，
这样做的对于消息消费者端只需要知道 从哪个topic ，哪个queue，哪个位置（queue offset ）开始消费就可以了，通过这个几个元素，就可以获取对应消息在
commitlog 的offset ，消息的大小，然后拿着commitlog offset与消息大小，就可以到commitlog获取到完整的消息。通过上面的介绍我们知道了consumeQueue
 是给消息消费者进行消费使用的，那么我们构建索引是干什么用的呢？比如说，我们想看看某个消息的被哪个消息消费者给消费的，或者是消息丢了，我们看看这个消息有没
 有被存到broker 上面，我们可以通过RocketMQ提供的可视化界面根据消息的topic，msgId或者topic，key进行查找，找到你想要的消息，BuildIndex你可以理解
 为往HashMap put 元素，它会根据 消息的topic与msgId 或者是topic与key 生成一个 key，然后将那个消息的commitlog的offset，key的hash值等元素封装
 成一个value，写到indexFile中

```
