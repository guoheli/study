### [TLS](https://jasonlees.netlify.app/article/tcpip-4-ssl-tls-2/) ###
![1](https://jasonlees.netlify.app/article/tcpip-4-SSL-TLS-2/tcpip-4-SSL-TLS-2-112851.png)
```JAVA
public static ProtocolMessageHandler<? extends ProtocolMessage> getHandler(TlsContext context,
        ProtocolMessageType protocolType, HandshakeMessageType handshakeType) {
        if (protocolType == null) {
            throw new RuntimeException("Cannot retrieve Handler, ProtocolMessageType is null");
        }
        try {
            switch (protocolType) {
                case HANDSHAKE:
                    HandshakeMessageType hmt = HandshakeMessageType.getMessageType(handshakeType.getValue());
                    return HandlerFactory.getHandshakeHandler(context, hmt);
                case CHANGE_CIPHER_SPEC:
                    return new ChangeCipherSpecHandler(context);
                case ALERT:
                    return new AlertHandler(context);
                case APPLICATION_DATA:
                    return new ApplicationMessageHandler(context);
                case HEARTBEAT:
                    return new HeartbeatMessageHandler(context);
                default:
                    return new UnknownMessageHandler(context, protocolType);
            }
        } catch (UnsupportedOperationException e) {
            // Could not get the correct handler, getting an
            // unknownMessageHandler instead(always successful)
            return new UnknownHandshakeHandler(context);
        }
    }
```

> 实现
```JAVA
public class NettySocketSSLHandler extends SimpleChannelInboundHandler<ByteBuffer>{  
     @Override  
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {  
            // Once session is secured, send a greeting and register the channel to the global channel  
            // list so the channel received the messages from others.  
            ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(  
                    new GenericFutureListener<Future<Channel>>() {  
                        @Override  
                        public void operationComplete(Future<Channel> future) throws Exception {  
                            if(future.isSuccess()){  
                                System.out.println("握手成功");  
                                byte[] array = new byte[]{ (byte)7d,  04} ;  
                                ByteBuffer bu = ByteBuffer.wrap(array) ;  
                                ctx.channel().writeAndFlush(bu) ;  
                            }else{  
                                System.out.println("握手失败");  
                            }  
                            ctx.writeAndFlush(  
                                    "Welcome to " + InetAddress.getLocalHost().getHostName() +  
                                            " secure chat service!\n");  
                            ctx.writeAndFlush(  
                                    "Your session is protected by " +  
                                            ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() +  
                                            " cipher suite.\n");  
  
                        }  
                    });  
        }  
    @Override  
    public void handlerAdded(ChannelHandlerContext ctx)  
        throws Exception {  
         System.out.println("服务端增加");  
    }  
      
    @Override  
    public void handlerRemoved(ChannelHandlerContext ctx){  
        System.out.println("移除:"+ctx.channel().remoteAddress());  
    }  
    @Override  
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {  
       System.out.println("Unexpected exception from downstream.");  
        ctx.close();  
    }  
    @Override  
    public void messageReceived(ChannelHandlerContext ctx, ByteBuffer msg) throws Exception {  
        System.out.println("服务端receive msg ");  
        byte[] array = new byte[]{00, 01, 00, 00, 00, 06, 05, 03, (byte)7d, 00, 00, 07} ;  
        ByteBuffer bu = ByteBuffer.wrap(array) ;  
        ctx.channel().writeAndFlush(bu) ;  
    }  
      
}  
```

```text
HTTP: TLS Session resumption should be enabled when using default BoringSSL #4130
TLS Session resumption is disabled by default in Netty as it triggers a Java bug that was only recently fixed (https://bugs.openjdk.java.net/browse/JDK-8241248). This issue only happens with Java's implementation of TLSv1.3, which is pretty rare IMHO (users use reverse-proxies and load balancers to handle TLS termination), so we should force it.


```

>+ [session ticket支持](https://blog.csdn.net/niaonao/article/details/102587396)
```text
1.3.6【javax.net.ssl】JSSE 程序无状态支持恢复会话
    TLS 服务器会将内部会话(session)信息以加密会话票证的形式，发送给支持无状态的客户端。该加密会话票证会在与TLS 握手期间，呈现给服务器，以恢复会话。

    新增加两个系统属性来支持该特性：
        client 用在客户端，用于在TLS 1.2的ClientHello 消息上切换会话票据扩展，false(默认) 不发送票据扩展， true 发送票据扩展。
        server 当客户端支持时，允许服务器使用无状态的会话票证，不支持无状态会话票证的客户端将使用缓存。false(默认) 不支持无状态，true 支持无状态。

jdk.tls.client.enableSessionTicketExtension: false
jdk.tls.server.enableSessionTicketExtension: false
```

>+ 测试验证
```text
When I try and scan my Java application using Qualys (https://www.ssllabs.com/ssltest/), it reports "No (IDs assigned but not accepted)" for "Session resumption (caching)", which implies it isn't enabled.

I have also tested this with OpenSSL, which has the same behavior as it has a different Session-ID for each request, but with session resumption it should be able to re-use the same Session-ID:

$ openssl s_client -connect 10.177.48.123:18899 -tls1_2 -reconnect -no_ticket | grep Session-ID
    Session-ID: 353DA62B469B16BB0156C366207AE4D4CD42A575AB94B376A0436B9AD7FE3D5F
    Session-ID: 0F30EB3EB602916D4B051BDC6887D81582C4698A63CE4F12FB5817AB0AC03CFB
    Session-ID: 6FA723129962DE82978A0ABF4345F4E4AD6E4987FFF3F9DA694BC7B5FF619A0F
    Session-ID: F5C43C5C627FB47C483087D5953DFF67912668019FF0A8235A0601E6E3C977E0
    Session-ID: 6E033003B9340BDBC97948546B4ED61AC4136E63203933E2D0A5A0DD2DC20408
    Session-ID: DE0BF90A519CA2854AC3AB87C138FF7D9EB27F420D0B75A88329725620206A6E
If I run the same test against "www.qualys.com", which does have session resumption enabled (update: they don't support Extended Master Secret Extension), then we can see the expected result:

$ openssl s_client -connect www.qualys.com:443 -tls1_2 -reconnect -no_ticket | grep Session-ID
    Session-ID: 58FF8EDA8BDD2A25905854C9AD2B19E1FE1549710CD8D86921A8A418D6877619
    Session-ID: 58FF8EDA8BDD2A25905854C9AD2B19E1FE1549710CD8D86921A8A418D6877619
    Session-ID: 58FF8EDA8BDD2A25905854C9AD2B19E1FE1549710CD8D86921A8A418D6877619
    Session-ID: 58FF8EDA8BDD2A25905854C9AD2B19E1FE1549710CD8D86921A8A418D6877619
    Session-ID: 58FF8EDA8BDD2A25905854C9AD2B19E1FE1549710CD8D86921A8A418D6877619
    Session-ID: 58FF8EDA8BDD2A25905854C9AD2B19E1FE1549710CD8D86921A8A418D6877619
```

openssl s_client -connect test1.www.local:443 --reconnect -no_ticket -CAfile ~/Keys/https/root/root.cer

### setting ###
```text
1. The SSLEngine will only resume sessions if you create it with SSLContext.createEngine(host, port). Otherwise it has no way of knowing who it's talking to, so no way of knowing what SSLSession to join.
2. SSL_OP_NO_TICKET default setting in netty, use SSLEngine clear, resolver in https://github.com/netty/netty/issues/6064
3. SslHandler.setHandshakeSuccess
```


### 核心实现类 ###
>+ testcase:SocketSslSessionReuseTest, OpenSslEngineTest, SSlEngineTest, SslHandlerTest
>+ handler/ReferenceCountedOpenSslContext
>+ handler/OpenSSlClientSessionCache OpenSslContext
>+ handler/OpenSslSessionContext
>+ 验证是否会话是否重用 SSLEngineTest.java #doHandshakeVerifyReusedAndClose, SslHandlerTest
>+  
![3](https://s2.51cto.com/oss/202101/26/83bf2e38863f1853da681f492c3f3956.png-wh_600x-s_1947235808.png)
### 核心讲解 ###
>+ https://www.cnblogs.com/hugetong/p/12192587.html
>+ https://blog.csdn.net/zhanglh046/article/details/120682742
>+ https://toutiao.io/posts/znvbg1/preview   tcp.stream eq 22(wireshark)
>+ https://moonbingbing.gitbooks.io/openresty-best-practices/content/ssl/session_resumption.html 
```
openssl s_client -connect test1.www.local:443 --reconnect -no_ticket -CAfile ~/Keys/https/root/root.cer

session ticket相比于session cache，是一种新的会话恢复机制。它的思想在于服务器去处它的所有会话数据（状态）并进行加密，再以票证的方式发回

客户端。在接下来的连接中，客户端将票证提交回服务器，由服务器检查票证的完整性，解密其内容，再使用其中的信息恢复会哈。这种方式

有可能使扩展服务器集群更为简单，因为如果不使用这种方式，就需要在服务集群的各个节点之间同步会话。（摘自<https权威指南>）

不过，需要额外提及的是。session ticket的引入，破坏了TLS的安全模型
```

### 参考 ###
>+ [java-secure-socket-extension-jsse-reference-guide](https://docs.oracle.com/en/java/javase/15/security/java-secure-socket-extension-jsse-reference-guide.html#GUID-0A438179-32A7-4900-A81C-29E3073E1E90)
>+ [JDK-8223922](https://bugs.openjdk.java.net/browse/JDK-8223922)
>+ [netty ticket time setting issue](https://github.com/netty/netty/issues/6064) 
>+ [netty issues](https://github.com/netty/netty/issues?q=+session+ticket) 
>+ [4](https://it.wenda123.org/question/stack/47211433/how-to-enable-session-resumption-on-netty-client-side) 


```text

本地调试
用 HttpClient 在本地写了简单的测试代码运行，发现同样报错，缩小了问题范围。关键点：设置 System.setProperty("javax.net.debug", "ssl"); 打开 ssl 的调试信息，可以获得相当多关键信息。在我的代码里，出错信息如下：

handling exception: javax.net.ssl.SSLHandshakeException:Remote host closed connection during handshake

网上搜索了之后逐渐发现了关键问题所在： 域名证书是正常的，但 SSL 握手失败，判断是 JDK 使用的 SSL 协议和加密算法与我们的 WebServer 不匹配。

SSL Server 兼容性检查
这里要祭出一个强力法器了 https://www.ssllabs.com/ssltest/ 这个工具可以对域名的证书做详细的检查，更实用的功能是可以反馈各种 User Agent 访问的可用性，把我们的域名丢进去检测一下，得分 A 还不错，再仔细看看下面的列表…jdk6 jdk7 都显示无法连接…简直是个大悲剧
```
#### 基础流程 ####
> 1、 创建一个CA
>> 2、 生成RSA密钥： openssl genrsa -out server.key 2048  
      ecparam密钥： openssl ecparam -name secp384r1 -genkey -out server-ecc.key
补充生成私钥和公钥： 
openssl ecparam -genkey -name prime256v1 -out server_key.key
openssl ecparam -genkey -name prime256v1 -out client_key.key

openssl ec -in server_key.key -pubout -out server_pubkey.pem
>>>3、通过密钥生成证书请求
openssl req -new -key server.key -out server.csr
openssl req -new -key server-ecc.key -out server-ecc.csr
>>>>4、 签发证书
openssl ca -in server.csr -out server.crt -days 3650
openssl ca -in server-ecc.csr -out server-ecc.crt -days 3650

#### 证书生成 ####
```text
1.生成ca证书
openssl req -new -x509 -keyout ca.key -out ca.crt -days 36500
在本目录得到 ca.key 和 ca.crt 文件

CA根证书替换方案：

 openssl genras -out ca.key 2048
 openssl req -new -key ca.key -out ca.csr
 openssl x509 -req -day 365 -in ca.csr -signkey ca.key -out ca.crt
 

2.生成服务端和客户端私钥
openssl genrsa -des3 -out server.key 1024
openssl genrsa -des3 -out client.key 1024

3.根据 key 生成 csr 文件
openssl req -new -key server.key -out server.csr
openssl req -new -key client.key -out client.csr

4.根据 ca 证书 server.csr 和 client.csr 生成 x509 证书
openssl x509 -req -days 36500 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt
openssl x509 -req -days 36500 -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt


openssl x509 -req -days 36500 -in client.csr -CA root.dev.crt -CAkey ca.key -CAcreateserial -out client.crt

5.将 key 文件进行 PKCS#8 编码
openssl pkcs8 -topk8 -in server.key -out pkcs8_server.key -nocrypt
openssl pkcs8 -topk8 -in client.key -out pkcs8_client.key -nocrypt

最后得到有用的文件分别为

服务器端： ca.crt、server.crt、pkcs8_server.key
客户端端： ca.crt、client.crt、pkcs8_client.key

-----------------------------------------------
![介绍](https://www.cxyzjd.com/article/lsslbh/108199111)
pem转其他
openssl x509 -in fullchain.pem -out fullchain.crt 
openssl rsa -in privkey.pem -out privkey.key  

其他转pem
openssl rsa -in temp.key -out temp.pem
openssl x509 -in tmp.crt -out tmp.pem


 cat server.key server.crt > server.pem
```

#### 查看命令 ####
```text
openssl 查看证书细节
打印证书的过期时间
openssl x509 -in signed.crt -noout -dates
打印出证书的内容：
openssl x509 -in cert.pem -noout -text    ****
打印出证书的系列号
openssl x509 -in cert.pem -noout -serial
打印出证书的拥有者名字
openssl x509 -in cert.pem -noout -subject
以RFC2253规定的格式打印出证书的拥有者名字
openssl x509 -in cert.pem -noout -subject -nameopt RFC2253
在支持UTF8的终端一行过打印出证书的拥有者名字
openssl x509 -in cert.pem -noout -subject -nameopt oneline -nameopt -escmsb
打印出证书的MD5特征参数
openssl x509 -in cert.pem -noout -fingerprint
打印出证书的SHA特征参数
openssl x509 -sha1 -in cert.pem -noout -fingerprint
把PEM格式的证书转化成DER格式
openssl x509 -in cert.pem -inform PEM -out cert.der -outform DER
把一个证书转化成CSR
openssl x509 -x509toreq -in cert.pem -out req.pem -signkey key.pem
给一个CSR进行处理，颁发字签名证书，增加CA扩展项
openssl x509 -req -in careq.pem -extfile openssl.cnf -extensions v3_ca -signkey key.pem -out cacert.pem
给一个CSR签名，增加用户证书扩展项
openssl x509 -req -in req.pem -extfile openssl.cnf -extensions v3_usr -CA cacert.pem -CAkey key.pem -CAcreateserial
查看csr文件细节：
openssl req -in my.csr -noout -text
```

### 链式证书 ###

