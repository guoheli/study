### 参考文献 ###

>+ [朱小厮](https://honeypps.com/categories/%E6%B6%88%E6%81%AF%E9%98%9F%E5%88%97/Kafka/)

### base概念 ###

>生产者发送类型：
   >+ sync、async; 交由producer.type指定
   >+ oneway; 由request.require.acks指定


对于异步模式，还有4个配套的参数，如下：
> <u>Property	         Default	Description </u>
>+ <u>queue.buffering.max.ms	5000	启用异步模式时，producer缓存消息的时间。比如我们设置成1000时，它会缓存1s的数据再一次发送出去，这样可以极大的增加broker吞吐量，但也会造成时效性的降低。</u>
>+ <u>queue.buffering.max.messages	10000	启用异步模式时，producer缓存队列里最大缓存的消息数量，如果超过这个值，producer就会阻塞或者丢掉消息。 </u>
>+ <u>queue.enqueue.timeout.ms	-1	当达到上面参数时producer会阻塞等待的时间。如果设置为0，buffer队列满时producer不会阻塞，消息直接被丢掉；若设置为-1，producer会被阻塞，不会丢消息。 </u>
>+ <u>batch.num.messages	200	启用异步模式时，一个batch缓存的消息数量。达到这个数值时，producer才会发送消息。（每次批量发送的数量）</u>

通过增加batch的大小，可以减少网络请求和磁盘IO的次数，当然具体参数设置需要在效率和时效性方面做一个权衡。在比较新的版本中还有batch.size这个参数，
request.require.acks返回leader收到确认的副本个数, -1: 可靠性最高, oneway即 acks=0的情况

```textmate
对于sync的发送方式：

producer.type=sync
request.required.acks=1
对于async的发送方式：

producer.type=async
request.required.acks=1
queue.buffering.max.ms=5000
queue.buffering.max.messages=10000
queue.enqueue.timeout.ms = -1
batch.num.messages=200
对于oneway的发送发送：

producer.type=async
request.required.acks=0
```
### kafka broke消息大小 ###
> message.max.bytes---指定消息的大小
### kafka 模式 ###
```textmate
所有的读写都是走leader节点， partition的顺序写保证高吞吐量, 同时实现了水平扩展
```
![架构](https://image.honeypps.com/images/papers/2017/213.png)

### 高可靠性存储分析 ###
Kafka从0.8.x版本开始提供partition级别的复制,replication的数量可以在$KAFKA_HOME/config/server.properties中配置（default.replication.refactor),
partition由多个segment组成, 这种特性也方便old segment的删除， segment的生命周期由服务端配置参数（log.segment.bytes, log.roll.{ms,hours}等参数决定
每一个文件都是由.index和.log组成, 每个文件的名称是上一个文件的最大offset
![partition struct](https://image.honeypps.com/images/papers/2017/215.png)

> 查找方式
```textmate
读取offset=170418的消息，首先查找segment文件，其中00000000000000000000.index为最开始的文件，
第二个文件为00000000000000170410.index（起始偏移为170410+1=170411），而第三个文件
为00000000000000239430.index（起始偏移为239430+1=239431），所以这个offset=170418就落到了第二个文件之中。
其他后续文件可以依次类推，以其实偏移量命名并排列这些文件，然后根据二分查找法就可以快速定位到具体文件位置。
其次根据00000000000000170410.index文件中的[8,1325]定位到00000000000000170410.log文件中的1325的位置
进行读取
```

### 复制和同步 ###
Kafka集群中有4个broker, 某topic有3个partition,且复制因子即副本个数也为3
![follower](https://image.honeypps.com/images/papers/2017/217.png)
```textmate
leader负责维护和跟踪ISR(In-Sync Replicas的缩写，表示副本同步队列，具体可参考下节)中所有follower滞后的状态。
当producer发送一条消息到broker后，leader写入消息并复制到所有follower。消息提交之后才被成功复制到所有的同步副本。
消息复制延迟受最慢的follower限制，重要的是快速检测慢副本，如果follower“落后”太多或者失效，leader将会把它从ISR
中删除。
```

ISR
```textmate
上节我们涉及到ISR (In-Sync Replicas)，这个是指副本同步队列。副本数对Kafka的吞吐率是有一定的影响，但极大的增强了可用性。默认情况下Kafka的replica数量为1，即每个partition都有一个唯一的leader，为了确保消息的可靠性，通常应用中将其值(由broker的参数offsets.topic.replication.factor指定)大小设置为大于1，比如3。 所有的副本（replicas）统称为Assigned Replicas，即AR。ISR是AR中的一个子集，由leader维护ISR列表，follower从leader同步数据有一些延迟（包括延迟时间replica.lag.time.max.ms和延迟条数replica.lag.max.messages两个维度, 当前最新的版本0.10.x中只支持replica.lag.time.max.ms这个维度），任意一个超过阈值都会把follower剔除出ISR, 存入OSR（Outof-Sync Replicas）列表，新加入的follower也会先存放在OSR中。AR=ISR+OSR。

Kafka 0.9.0.0版本后移除了replica.lag.max.messages参数，只保留了replica.lag.time.max.ms作为ISR中副本管理的
参数。为什么这样做呢？replica.lag.max.messages表示当前某个副本落后leader的消息数量超过了这个参数的值，那么leader
就会把follower从ISR中删除。假设设置replica.lag.max.messages=4，那么如果producer一次传送至broker的消息数量都小
于4条时，因为在leader接受到producer发送的消息之后而follower副本开始拉取这些消息之前，follower落后leader的消息数
不会超过4条消息，故此没有follower移出ISR，所以这时候replica.lag.max.message的设置似乎是合理的。但是producer发
起瞬时高峰流量，producer一次发送的消息超过4条时，也就是超过replica.lag.max.messages，此时follower都会被认为是
与leader副本不同步了，从而被踢出了ISR。但实际上这些follower都是存活状态的且没有性能问题。那么在之后追上leader,并被
重新加入了ISR。于是就会出现它们不断地剔出ISR然后重新回归ISR，这无疑增加了无谓的性能损耗。而且这个参数是broker全局的。
设置太大了，影响真正“落后”follower的移除；设置的太小了，导致follower的频繁进出。无法给定一个合适的
replica.lag.max.messages的值，故此，新版本的Kafka移除了这个参数。
```

### 重平衡 ###


### 如何保证不丢数据 ###
![partition的组成](https://image.honeypps.com/images/papers/2017/216.png)

一、什么时候真正消费的问题？
```textmate
每个replica都有HW,leader和follower各自负责更新自己的HW的状态。对于leader新写入的消息，consumer不能立刻消费，
leader会等待该消息被所有ISR中的replicas同步后更新HW，此时消息才能被consumer消费。这样就保证了如果leader所在的
broker失效，该消息仍然可以从新选举的leader中获取。对于来自内部broker的读取请求，没有HW的限制。follower异步的从
leader复制数据，数据只要被leader写入log就被认为已经commit，这种情况下如果follower都还没有复制完，落后于leader
时，突然leader宕机，则会丢失数据。而Kafka的这种使用ISR的方式则很好的均衡了确保数据不丢失以及吞吐率
```
二、acks保证上面的ISR的维护，acks=-1情况，可能出现重复消息的问题，当然上图中如果在leader crash的时候，follower2还没有同步到任何数据，
而且follower2被选举为新的leader的话，这样消息就不会重复。

![d](https://image.honeypps.com/images/papers/2017/221.png)

? 有没有什么方式做到不重复消费的问题

### ISR数据的维护 ###

Kafka的ISR的管理最终都会反馈到Zookeeper节点上。具体位置为：/brokers/topics/[topic]/partitions/[partition]/state。
目前有两个地方会对这个Zookeeper的节点进行维护：

>+ Controller来维护：Kafka集群中的其中一个Broker会被选举为Controller，主要负责Partition管理和副本状态管理，也会执行类似于重分配partition之类的管理任务。在符合某些特定条件下，Controller下的LeaderSelector会选举新的leader，ISR和新的leader_epoch及controller_epoch写入Zookeeper的相关节点中。同时发起LeaderAndIsrRequest通知所有的replicas。
>+ leader来维护：leader有单独的线程定期检测ISR中follower是否脱离ISR, 如果发现ISR变化，则会将新的ISR的信息返回到Zookeeper的相关节点中。

### leader的选举 ###
```textmate
一条消息只有被ISR中的所有follower都从leader复制过去才会被认为已提交。这样就避免了部分数据被写进了leader，还没来得及被任何follower复制就宕机了，而造成数据丢失。而对于producer而言，它可以选择是否等待消息commit，这可以通过request.required.acks来设置。这种机制确保了只要ISR中有一个或者以上的follower，一条被commit的消息就不会丢失。

有一个很重要的问题是当leader宕机了，怎样在follower中选举出新的leader，因为follower可能落后很多或者直接crash了，所以必须确保选择“最新”的follower作为新的leader。一个基本的原则就是，如果leader不在了，新的leader必须拥有原来的leader commit的所有消息。这就需要做一个折中，如果leader在一个消息被commit前等待更多的follower确认，那么在它挂掉之后就有更多的follower可以成为新的leader，但这也会造成吞吐率的下降。

一种非常常用的选举leader的方式是“少数服从多数”，Kafka并不是采用这种方式。这种模式下，如果我们有2f+1个副本，那么在commit之前必须保证有f+1个replica复制完消息，同时为了保证能正确选举出新的leader，失败的副本数不能超过f个。这种方式有个很大的优势，系统的延迟取决于最快的几台机器，也就是说比如副本数为3，那么延迟就取决于最快的那个follower而不是最慢的那个。“少数服从多数”的方式也有一些劣势，为了保证leader选举的正常进行，它所能容忍的失败的follower数比较少，如果要容忍1个follower挂掉，那么至少要3个以上的副本，如果要容忍2个follower挂掉，必须要有5个以上的副本。也就是说，在生产环境下为了保证较高的容错率，必须要有大量的副本，而大量的副本又会在大数据量下导致性能的急剧下降。这种算法更多用在Zookeeper这种共享集群配置的系统中而很少在需要大量数据的系统中使用的原因。HDFS的HA功能也是基于“少数服从多数”的方式，但是其数据存储并不是采用这样的方式。

实际上，leader选举的算法非常多，比如Zookeeper的Zab、Raft以及Viewstamped Replication。而Kafka所使用的leader选举算法更像是微软的PacificA算法。
```
### 高可靠性使用分析 ###
##### 4.1 消息传输保障 ######
前面已经介绍了Kafka如何进行有效的存储，以及了解了producer和consumer如何工作。接下来讨论的是Kafka如何确保消息在producer和consumer之间传输。有以下三种可能的传输保障（delivery guarantee）:

>+ At most once: 消息可能会丢，但绝不会重复传输
>+ At least once：消息绝不会丢，但可能会重复传输
>+ Exactly once：每条消息肯定会被传输一次且仅传输一次

```Kafka的消息传输保障机制非常直观。当producer向broker发送消息时，一旦这条消息被commit，由于副本机制（replication）的存在，它就不会丢失。但是如果producer发送数据给broker后，遇到的网络问题而造成通信中断，那producer就无法判断该条消息是否已经提交（commit）。虽然Kafka无法确定网络故障期间发生了什么，但是producer可以retry多次，确保消息已经正确传输到broker中，所以目前Kafka实现的是at least once。

consumer从broker中读取消息后，可以选择commit，该操作会在Zookeeper中存下该consumer在该partition下读取的消息的offset。该consumer下一次再读该partition时会从下一条开始读取。如未commit，下一次读取的开始位置会跟上一次commit之后的开始位置相同。当然也可以将consumer设置为autocommit，即consumer一旦读取到数据立即自动commit。如果只讨论这一读取消息的过程，那Kafka是确保了exactly once, 但是如果由于前面producer与broker之间的某种原因导致消息的重复，那么这里就是at least once。

考虑这样一种情况，当consumer读完消息之后先commit再处理消息，在这种模式下，如果consumer在commit后还没来得及处理消息就crash了，下次重新开始工作后就无法读到刚刚已提交而未处理的消息，这就对应于at most once了。

读完消息先处理再commit。这种模式下，如果处理完了消息在commit之前consumer crash了，下次重新开始工作时还会处理刚刚未commit的消息，实际上该消息已经被处理过了，这就对应于at least once。

要做到exactly once就需要引入消息去重机制。
```

##### 4.2 消息去重 #####

```textmate
如上一节所述，Kafka在producer端和consumer端都会出现消息的重复，这就需要去重处理。

Kafka文档中提及GUID(Globally Unique Identifier)的概念，通过客户端生成算法得到每个消息的unique id，同时可映射至broker上存储的地址，即通过GUID便可查询提取消息内容，也便于发送方的幂等性保证，需要在broker上提供此去重处理模块，目前版本尚不支持。

针对GUID, 如果从客户端的角度去重，那么需要引入集中式缓存，必然会增加依赖复杂度，另外缓存的大小难以界定。
 
不只是Kafka, 类似RabbitMQ以及RocketMQ这类商业级中间件也只保障at least once, 且也无法从自身去进行消息去重。所以我们建议业务方根据自身的业务特点进行去重，比如业务消息本身具备幂等性，或者借助Redis等其他产品进行去重处理。
```

##### 4.3 高可靠性配置 ####

```
Kafka提供了很高的数据冗余弹性，对于需要数据高可靠性的场景，我们可以增加数据冗余备份数（replication.factor），调高最小写入副本数的个数（min.insync.replicas）等等，但是这样会影响性能。反之，性能提高而可靠性则降低，用户需要自身业务特性在彼此之间做一些权衡性选择。

要保证数据写入到Kafka是安全的，高可靠的，需要如下的配置：

topic的配置：replication.factor>=3,即副本数至少是3个；2<=min.insync.replicas<=replication.factor
broker的配置：leader的选举条件unclean.leader.election.enable=false
producer的配置：request.required.acks=-1(all)，producer.type=sync
```

### kafka压缩 ###
```textmate
Kafka(本文是以0.8.2.x的版本做基准的)本身可以支持几种类型的压缩，比如gzip和snappy，更高的版本还支持lz4。默认
是none,即不采用任何压缩。开启压缩的方式是在客户端调用的时候设置producer的参数。与压缩有关的参数有
```
CODE:
```JAVA
Properties properties = new Properties();
properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
properties.put("bootstrap.servers", brokerList);
properties.put("compression.type", "gzip");

Producer<String,byte[]> producer = new KafkaProducer<String,byte[]>(properties);

ProducerRecord<String,byte[]> producerRecord = new ProducerRecord<String,byte[]>(topic, "messages".getBytes());
Future<RecordMetadata> future =  producer.send(producerRecord, new Callback() 
{
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        System.out.println(metadata.offset());
    }
});
```

### [KAFKA端到端审计](https://honeypps.com/mq/kafka-end-to-end-audit/) ###
```text
Kafka端到端审计是指生产者生产的消息存入至broker，以及消费者从broker中消费消息这个过程之间消息个数及延迟的审计，以此可以检测是否有数据丢失，是否有数据重复以及端到端的延迟等。

目前主要调研了3个产品：

  Chaperone (Uber)
  Confluent Control Center（非开源，收费）
  Kafka Monitor (LinkedIn)

对于Kafka端到端的审计主要通过：

  消息payload中内嵌时间戳timestamp
  消息payload中内嵌全局index 
  消息payload中内嵌timestamp和index
```

### PRODUCER 拦截器 ###

```JAVA
properties.put("interceptor.classes", "com.hidden.producer.ProducerInterceptorDemo,com.hidden.producer.ProducerInterceptorDemoPlus");
```

### KAFKA消息序列化和反序列化 ###

Kafka Producer在发送消息时必须配置的参数为：bootstrap.servers、key.serializer、value.serializer。序列化操作
是在拦截器（Interceptor）执行之后并且在分配分区(partitions)之前执行的。

```JAVA
Properties properties = new Properties();
properties.put("bootstrap.servers", brokerList);
properties.put("group.id", consumerGroup);
properties.put("session.timeout.ms", 10000);
properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
properties.put("value.deserializer", "com.hidden.client.DemoDeserializer");
properties.put("client.id", "hidden-consumer-client-id-zzh-2");
KafkaConsumer<String, Company> consumer = new KafkaConsumer<String, Company>(properties);
consumer.subscribe(Arrays.asList(topic));
try {
    while (true) {
        ConsumerRecords<String, Company> records = consumer.poll(100);
        for (ConsumerRecord<String, Company> record : records) {
            String info = String.format("topic=%s, partition=%s, offset=%d, consumer=%s, country=%s",
                    record.topic(), record.partition(), record.offset(), record.key(), record.value());
            System.out.println(info);
        }
        consumer.commitAsync(new OffsetCommitCallback() {
            public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                if (exception != null) {
                    String error = String.format("Commit failed for offsets {}", offsets, exception);
                    System.out.println(error);
                }
            }
        });
    }
} finally {
    consumer.close();
}
```
有些时候自定义的类型还可以和Avro、ProtoBuf等联合使用，而且这样更加的方便快捷，比如我们将前面Company的Serializer
和Deserializer用Protostuff包装一下，详细参考如下：
```java
public byte[] serialize(String topic, Company data) {
    if (data == null) {
        return null;
    }
    Schema schema = (Schema) RuntimeSchema.getSchema(data.getClass());
    LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
    byte[] protostuff = null;
    try {
        protostuff = ProtostuffIOUtil.toByteArray(data, schema, buffer);
    } catch (Exception e) {
        throw new IllegalStateException(e.getMessage(), e);
    } finally {
        buffer.clear();
    }
    return protostuff;
}

public Company deserialize(String topic, byte[] data) {
    if (data == null) {
        return null;
    }
    Schema schema = RuntimeSchema.getSchema(Company.class);
    Company ans = new Company();
    ProtostuffIOUtil.mergeFrom(data, ans, schema);
    return ans;
}
```

### 自定义分区 ###
默认分区获取：List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
>+ 配置设置： properties.put("partitioner.class","com.hidden.partitioner.DemoPartitioner");
```java
public class DemoPartitioner implements Partitioner {

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    @Override
    public void configure(Map<String, ?> configs) {}

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        if (null == keyBytes || keyBytes.length<1) {
            return atomicInteger.getAndIncrement() % numPartitions;
        }
        //借用String的hashCode的计算方式
        int hash = 0;
        for (byte b : keyBytes) {
            hash = 31 * hash + b;
        }
        return hash % numPartitions;
    }

    @Override
    public void close() {}
}
```
### 副本 ###
```text
通常情况下，Kafka中的每个分区（partition）都会分配多个副本（replica），具体的副本数量由Broker级别
参数default.replication.factor（默认大小为1）指定，也可以在创建topic的时候通过 
–replication-factor ${num} 显式指定副本的数量（副本因子）。一般情况下，将前者default.replication.factor
设置为大于1的值，这样在参数auto.create.topic.enable为true的时候，自动创建的topic会根据default.replication.factor
的值来创建副本数；或者更加通用的做法是使用后者而指定大于1的副本数。

每个分区的多个副本称之为AR（assigned replicas），包含至多一个leader副本和多个follower副本。与AR对应的另一个重要的概念就是ISR（in-sync replicas），ISR是指与leader副本保持同步状态的副本集合，当然leader副本本身也是这个集合中的一员。而ISR之外，也就是处于同步失败或失效状态的副本，副本对应的分区也就称之为同步失效分区，即under-replicated分区。
```
失效副本的判定
```thymeleafexpressions 怎么样判定一个分区是否有副本是处于同步失效状态的呢？从Kafka 0.9.x版本开始通过唯一的一个参数replica.lag.time.max.ms
（默认大小为10,000）来控制，当ISR中的一个follower副本滞后leader副本的时间超过参数replica.lag.time.max.ms
指定的值时即判定为副本失效，需要将此follower副本剔出除ISR之外。具体实现原理很简单，当follower副本将leader副本
的LEO（Log End Offset，每个分区最后一条消息的位置）之前的日志全部同步时，则认为该follower副本已经追赶上leader副本，
此时更新该副本的lastCaughtUpTimeMs标识。Kafka的副本管理器（ReplicaManager）启动时会启动一个副本过期检测的定时任务，
而这个定时任务会定时检查当前时间与副本的lastCaughtUpTimeMs差值是否大于参数replica.lag.time.max.ms指定的值。
千万不要错误的认为follower副本只要拉取leader副本的数据就会更新lastCaughtUpTimeMs，试想当leader副本的消息流入
速度大于follower副本的拉取速度时，follower副本一直不断的拉取leader副本的消息也不能与leader副本同步，如果还将此
follower副本置于ISR中，那么当leader副本失效，而选取此follower副本为新的leader副本，那么就会有严重的消息丢失。
```


### topic创建 ###

在使用kafka发送消息和消费消息之前，必须先要创建topic，在kafka中创建topic的方式有以下2种：

如果kafka broker中的config/server.properties配置文件中配置了auto.create.topics.enable参数为true（默认值就是true），那么当生产者向一个尚未创建的topic发送消息时，会自动创建一个num.partitions（默认值为1）个分区和default.replication.factor（默认值为1）个副本的对应topic。不过我们一般不建议将auto.create.topics.enable参数设置为true，因为这个参数会影响topic的管理与维护。
通过kafka提供的kafka-topics.sh脚本来创建，并且我们也建议通过这种方式（或者相关的变种方式）来创建topic。
举个demo：通过kafka-topics.sh脚本来创建一个名为topic-test1并且副本数为2、分区数为4的topic。（如无特殊说明，本文所述都是基于1.0.0版本。）

```SH
bin/kafka-topics.sh --create --zookeeper 192.168.0.2:2181/kafka100 --topic topic-test1 --replication-factor 2 --partitions 4
```


### 消息堆积 ###
![d](https://image.honeypps.com/images/papers/2018/114.png)

```thymeleafexpressions
首先来说说ConsumerOffset，Kafka中有两处可以存储，一个是Zookeeper，而另一个是”consumer_offsets这个内部topic中
，前者是0.8.x版本中的使用方式，但是随着版本的迭代更新，现在越来越趋向于后者。就拿1.0.0版本来说，虽然默认是存储在
”consumer_offsets”中，但是保不齐用于就将其存储在了Zookeeper中了。这个问题倒也不难解决，针对两种方式都去拉取，
然后哪个有值的取哪个。不过这里还有一个问题，对于消费位移来说，其一般不会实时的更新，而更多的是定时更新，这样可以提高
整体的性能。那么这个定时的时间间隔就是ConsumerOffset的误差区间之一。
```

### 消息格式演进 ###

![v0](https://image.honeypps.com/images/papers/2018/118.png)
```textmate
下面来具体陈述一下消息（Record）格式中的各个字段，从crc32开始算起，各个字段的解释如下：

crc32（4B）：crc32校验值。校验范围为magic至value之间。
magic（1B）：消息格式版本号，此版本的magic值为0。
attributes（1B）：消息的属性。总共占1个字节，低3位表示压缩类型：0表示NONE、1表示GZIP、2表示SNAPPY、3表示LZ4（LZ4自Kafka 0.9.x引入），其余位保留。
key length（4B）：表示消息的key的长度。如果为-1，则表示没有设置key，即key=null。
key：可选，如果没有key则无此字段。
value length（4B）：实际消息体的长度。如果为-1，则表示消息为空。
value：消息体。可以为空，比如tomnstone消息。
v0版本中一个消息的最小长度（RECORD_OVERHEAD_V0）为crc32 + magic + attributes + key length + value length = 4B + 1B + 1B + 4B + 4B =14B，也就是说v0版本中一条消息的最小长度为14B，如果小于这个值，那么这就是一条破损的消息而不被接受。
```

![v1](https://image.honeypps.com/images/papers/2018/119.png)
```text
常见的压缩算法是数据量越大压缩效果越好，一条消息通常不会太大，这就导致压缩效果并不太好。而kafka实现的压缩方式是将多条
消息一起进行压缩，这样可以保证较好的压缩效果。而且在一般情况下，生产者发送的压缩数据在kafka broker中也是保持压缩状态
进行存储，消费者从服务端获取也是压缩的消息，消费者在处理消息之前才会解压消息，这样保持了端到端的压缩。
```
![压缩](https://image.honeypps.com/images/papers/2018/121.png)
![v2](https://image.honeypps.com/images/papers/2018/122.png)

### [日志清理](https://honeypps.com/mq/log-deletion-of-kafka-log-retention/) ###

```text
Kafka提供了两种日志清理策略：

日志删除（Log Deletion）：按照一定的保留策略来直接删除不符合条件的日志分段。
日志压缩（Log Compaction）：针对每个消息的key进行整合，对于有相同key的的不同value值，只保留最后一个版本。
我们可以通过broker端参数log.cleanup.policy来设置日志清理策略，此参数默认值为“delete”，即采用日志删除的
清理策略。如果要采用日志压缩的清理策略的话，就需要将log.cleanup.policy设置为“compact”，并且还需要将log.cleaner.enable（默认值为true）设定为true。通过将log.cleanup.policy参数设置为“delete,compact”还可以同时支持日志删除和日志压缩两种策略。日志清理的粒度可以控制到topic级别，比如与log.cleanup.policy对应的主题级别的参数为cleanup.policy，为了简化说明，本文只采用broker端参数做陈述，如若需要topic级别的参数可以查
```

### unclean.leader.election.enable参数true变false ###
![d](https://image.honeypps.com/images/papers/2018/133.png)

为true,则会把follower2的作为选举，出现消息丢失的情况
![2](https://image.honeypps.com/images/papers/2018/135.png)
![3](https://image.honeypps.com/images/papers/2018/136.png)


### [kafka心脏-控制器](https://honeypps.com/mq/kafka-controller-analysis/) ###

```text
在Kafka集群中会有一个或者多个broker，其中有一个broker会被选举为控制器（Kafka Controller），它负责管理整个集群中所有分区和副本的状态。当某个分区的leader副本出现故障时，由控制器负责为该分区选举新的leader副本。当检测到某个分区的ISR集合发生变化时，由控制器负责通知所有broker更新其元数据信息。当使用kafka-topics.sh脚本为某个topic增加分区数量时，同样还是由控制器负责分区的重新分配
```
![2](https://image.honeypps.com/images/papers/2018/137.png)
```text
在Kafka的早期版本中，并没有采用Kafka Controller这样一个概念来对分区和副本的状态进行管理，而是依赖于Zookeeper，每个broker都会在Zookeeper上为分区和副本注册大量的监听器（Watcher）。当分区或者副本状态变化时，会唤醒很多不必要的监听器，这种严重依赖于Zookeeper的设计会有脑裂、羊群效应以及造成Zookeeper过载的隐患。在目前的新版本的设计中，只有Kafka Controller在Zookeeper上注册相应的监听器，其他的broker极少需要再监听Zookeeper中的数据变化，这样省去了很多不必要的麻烦。不过每个broker还是会对/controller节点添加监听器的，以此来监听此节点的数据变化（参考ZkClient中的IZkDataListener）
```

### 分区路由 ###

![1](https://image.honeypps.com/images/papers/2018/138.png)
![2](https://image.honeypps.com/images/papers/2018/139.png)

##### [RangeAssignor分配策略](https://honeypps.com/mq/kafka-partitions-allocation-strategy-1-range-assignor/) ####

#### [RoundRobinAssignor](https://honeypps.com/mq/kafka-partitions-allocation-strategy-2-round-robin-and-sticky-assignor/) ####


### kafka分区只能增加不能减少 ###
```text
 bin/kafka-topics.sh --zookeeper localhost:2181/kafka --alter --topic topic-config --partitions 3

注意上面提示的告警信息：当主题中的消息包含有key时（即key不为null），根据key来计算分区的行为就会有所影响。当topic-config的分区数为1时，不管消息的key为何值，消息都会发往这一个分区中；当分区数增加到3时，那么就会根据消息的key来计算分区号，原本发往分区0的消息现在有可能会发往分区1或者分区2中。如此还会影响既定消息的顺序，所以在增加分区数时一定要三思而后行。对于基于key计算的主题而言，建议在一开始就设置好分区数量，避免以后对其进行调整。
```


### [分区越多吞吐越高？](https://honeypps.com/mq/is-that-the-more-partitions-in-kafka-topic-the-higher-throughout/) ###
开始随着分区数的增加相应的吞吐量也会有多增长。一旦分区数超过了某个阈值之后整体的吞吐量也同样是不升反降的，同样说明了分区数越多并不会使得吞吐量一直增长

生产者测试：
```text
使用kafka-producer-perf-test.sh脚本分别往这些主题中发送100万条消息体大小为1KB的消息，相对应的测试命令如下：

bin/kafka-producer-perf-test.sh --topic topic-xxx 
--num-records 1000000 --record-size 1024 
--throughput 100000000 --producer-props 
bootstrap.servers=localhost:9092 acks=1
```
消费者测试： 
```text
对于消息消费者而言同样也有吞吐量方面的考量。使用kafka-consumer-perf-test.sh脚本分别消费这些主题中的100万条消息，相对应的测试命令如下：

bin/kafka-consumer-perf-test.sh --topic topic-xxx 
--messages 1000000 --broker-list localhost:9092
消费者性能测试的结果如下图所示。与生产者性能测试相同的是，对于不同的测试环境或者不同的测试批次所得到的测试结果也不尽相同，但总体趋势还是会保持和图中的一样。
```

### [主题删除重建](https://honeypps.com/mq/the-secret-behind-kafka-topic-deletion/) ###
在新版的消费者中将消费位移存储在了Kafka内部的主题__consumer_offsets中。每当消费者有消费位移提交时，会通过
OffsetCommitRequest请求将所提交的位移发送给消费者所属消费组对应的组协调器GroupCoordinator中，组协调器
GroupCoordinator会将消费位移存入到__consumer_offsets主题中，同时也会在内存中保留一份备份。Kafka重启
之后会将__consumer_offsets中所有的消息保存到内存中，即保存到各个GroupCoordinator中来进行维护。


### 分区、控制器、消费者leader 选举 ###

```text
控制器的选举
在Kafka集群中会有一个或多个broker，其中有一个broker会被选举为控制器（Kafka Controller），它负责管理整个集群中所有分区和副本的状态等工作。比如当某个分区的leader副本出现故障时，由控制器负责为该分区选举新的leader副本。再比如当检测到某个分区的ISR集合发生变化时，由控制器负责通知所有broker更新其元数据信息。

Kafka Controller的选举是依赖Zookeeper来实现的，在Kafka集群中哪个broker能够成功创建/controller这个临时（EPHEMERAL）节点他就可以成为Kafka Controller。

这里需要说明一下的是Kafka Controller的实现还是相当复杂的，涉及到各个方面的内容，如果你掌握了Kafka Controller，你就掌握了Kafka的“半壁江山”。篇幅所限，这里就不一一展开了，有兴趣的读者可以查阅一下《深入理解Kafka》中第6章的相关内容。

分区leader的选举
这里不说什么一致性协议（PacificA）相关的内容，只讲述具体的选举内容。

分区leader副本的选举由Kafka Controller 负责具体实施。当创建分区（创建主题或增加分区都有创建分区的动作）或分区上线（比如分区中原先的leader副本下线，此时分区需要选举一个新的leader上线来对外提供服务）的时候都需要执行leader的选举动作。

基本思路是按照AR集合中副本的顺序查找第一个存活的副本，并且这个副本在ISR集合中。一个分区的AR集合在分配的时候就被指定，并且只要不发生重分配的情况，集合内部副本的顺序是保持不变的，而分区的ISR集合中副本的顺序可能会改变。注意这里是根据AR的顺序而不是ISR的顺序进行选举的。这个说起来比较抽象，有兴趣的读者可以手动关闭/开启某个集群中的broker来观察一下具体的变化。

还有一些情况也会发生分区leader的选举，比如当分区进行重分配（reassign）的时候也需要执行leader的选举动作。这个思路比较简单：从重分配的AR列表中找到第一个存活的副本，且这个副本在目前的ISR列表中。

再比如当发生优先副本（preferred replica partition leader election）的选举时，直接将优先副本设置为leader即可，AR集合中的第一个副本即为优先副本。

Kafka中有很多XX副本的称呼，如果不是很了解，可以关注本系列的下一篇《Kafka科普系列 | Kafka中到底有多少种副本？》

还有一种情况就是当某节点被优雅地关闭（也就是执行ControlledShutdown）时，位于这个节点上的leader副本都会下线，所以与此对应的分区需要执行leader的选举。这里的具体思路为：从AR列表中找到第一个存活的副本，且这个副本在目前的ISR列表中，与此同时还要确保这个副本不处于正在被关闭的节点上。

消费者相关的选举
对于这部分内容的理解，额。。如果你对消费者、消费组、消费者协调器以及组协调器不甚理解的话，那么。。。职能毛遂自荐《深入理解Kafka》一书了，嘿嘿。

组协调器GroupCoordinator需要为消费组内的消费者选举出一个消费组的leader，这个选举的算法也很简单，分两种情况分析。如果消费组内还没有leader，那么第一个加入消费组的消费者即为消费组的leader。如果某一时刻leader消费者由于某些原因退出了消费组，那么会重新选举一个新的leader，这个重新选举leader的过程又更“随意”了，相关代码如下：

//scala code.
private val members = new mutable.HashMap[String, MemberMetadata]
var leaderId = members.keys.head
解释一下这2行代码：在GroupCoordinator中消费者的信息是以HashMap的形式存储的，其中key为消费者的member_id，而value是消费者相关的元数据信息。leaderId表示leader消费者的member_id，它的取值为HashMap中的第一个键值对的key，这种选举的方式基本上和随机无异。总体上来说，消费组的leader选举过程是很随意的。

插播：近日发现文章被盗的厉害，发文几个小时文章就在各大门户网站上出现，全都是标的原创。虽然没有能力制止，但是我发现大多不仔细看直接抄的（有连我下面的公众号二维码也抄了去的，当然也有用PS来P掉我图片水印的鸡贼操作），所以机智的我。。不如在文章中插播一下我的书，让他们盗了去也好替我宣传宣传。我是皮的很~~

到这里就结束了吗？还有分区分配策略的选举呢。

许你对此有点陌生，但是用过Kafka的同学或许对partition.assignment.strategy（取值为RangeAssignor、RoundRobinAssignor、StickyAssignor等）这个参数并不陌生。每个消费者都可以设置自己的分区分配策略，对消费组而言需要从各个消费者呈报上来的各个分配策略中选举一个彼此都“信服”的策略来进行整体上的分区分配。这个分区分配的选举并非由leader消费者决定，而是根据消费组内的各个消费者投票来决定的
```

### kafka的分区分配 ###

生产者分区分配， 默认使用DefaultPartitioner， 消费者的分区分配而言，Kafka自身提供了三种策略，分别为RangeAssignor、RoundRobinAssignor以及StickyAssignor，其中RangeAssignor为默认的分区分配策略，


### [事务](https://honeypps.com/mq/kafka-basic-knowledge-of-transaction/) ###
![2](https://image.honeypps.com/images/papers/2019/111.png)


### 面试相关 ###
```text
有很多人问过我要过Kafka相关的面试题，我一直懒得整理，这几天花了点时间，结合之前面试被问过的、别人咨询过的、我会问别人的进行了相关的整理，也就几十题，大家花个几分钟看看应该都会。面试题列表如下：

Kafka的用途有哪些？使用场景如何？
Kafka中的ISR、AR又代表什么？ISR的伸缩又指什么
Kafka中的HW、LEO、LSO、LW等分别代表什么？
Kafka中是怎么体现消息顺序性的？
Kafka中的分区器、序列化器、拦截器是否了解？它们之间的处理顺序是什么？
Kafka生产者客户端的整体结构是什么样子的？
Kafka生产者客户端中使用了几个线程来处理？分别是什么？
Kafka的旧版Scala的消费者客户端的设计有什么缺陷？
“消费组中的消费者个数如果超过topic的分区，那么就会有消费者消费不到数据”这句话是否正确？如果正确，那么有没有什么hack的手段？
消费者提交消费位移时提交的是当前消费到的最新消息的offset还是offset+1?
有哪些情形会造成重复消费？
那些情景下会造成消息漏消费？
KafkaConsumer是非线程安全的，那么怎么样实现多线程消费？
简述消费者与消费组之间的关系
当你使用kafka-topics.sh创建（删除）了一个topic之后，Kafka背后会执行什么逻辑？
topic的分区数可不可以增加？如果可以怎么增加？如果不可以，那又是为什么？
topic的分区数可不可以减少？如果可以怎么减少？如果不可以，那又是为什么？
创建topic时如何选择合适的分区数？
Kafka目前有那些内部topic，它们都有什么特征？各自的作用又是什么？
优先副本是什么？它有什么特殊的作用？
Kafka有哪几处地方有分区分配的概念？简述大致的过程及原理
简述Kafka的日志目录结构
Kafka中有那些索引文件？
如果我指定了一个offset，Kafka怎么查找到对应的消息？
如果我指定了一个timestamp，Kafka怎么查找到对应的消息？
聊一聊你对Kafka的Log Retention的理解
聊一聊你对Kafka的Log Compaction的理解
聊一聊你对Kafka底层存储的理解（页缓存、内核层、块层、设备层）
聊一聊Kafka的延时操作的原理
聊一聊Kafka控制器的作用
消费再均衡的原理是什么？（提示：消费者协调器和消费组协调器）
Kafka中的幂等是怎么实现的
Kafka中的事务是怎么实现的（这题我去面试6加被问4次，照着答案念也要念十几分钟，面试官简直凑不要脸）
Kafka中有那些地方需要选举？这些地方的选举策略又有哪些？
失效副本是指什么？有那些应对措施？
多副本下，各个副本中的HW和LEO的演变过程
为什么Kafka不支持读写分离？
Kafka在可靠性方面做了哪些改进？（HW, LeaderEpoch）
Kafka中怎么实现死信队列和重试队列？
Kafka中的延迟队列怎么实现（这题被问的比事务那题还要多！！！听说你会Kafka，那你说说延迟队列怎么实现？）
Kafka中怎么做消息审计？
Kafka中怎么做消息轨迹？
Kafka中有那些配置参数比较有意思？聊一聊你的看法
Kafka中有那些命名比较有意思？聊一聊你的看法
Kafka有哪些指标需要着重关注？
怎么计算Lag？(注意read_uncommitted和read_committed状态下的不同)
Kafka的那些设计让它有如此高的性能？
Kafka有什么优缺点？
还用过什么同质类的其它产品，与Kafka相比有什么优缺点？
为什么选择Kafka?
在使用Kafka的过程中遇到过什么困难？怎么解决的？
怎么样才能确保Kafka极大程度上的可靠性？
聊一聊你对Kafka生态的理解
```

### [时间轮（timingWheel)](https://honeypps.com/mq/kafka-analysis-of-timing-wheel/) ###

![1](https://image.honeypps.com/images/papers/2018/130.png)
![2](https://image.honeypps.com/images/papers/2018/131.png)