package io.netty.handler.ssl;


import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.internal.tcnative.SSLContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static io.netty.internal.tcnative.SSL.SSL_OP_NO_TICKET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class SslHandlerTest {

    // Protocols
    static final String PROTOCOL_SSL_V2_HELLO = "SSLv2Hello";
    static final String PROTOCOL_SSL_V2 = "SSLv2";
    static final String PROTOCOL_SSL_V3 = "SSLv3";
    static final String PROTOCOL_TLS_V1 = "TLSv1";
    static final String PROTOCOL_TLS_V1_1 = "TLSv1.1";
    static final String PROTOCOL_TLS_V1_2 = "TLSv1.2";
    static final String PROTOCOL_TLS_V1_3 = "TLSv1.3";

    @Test
    public void testSessionTicketsWithTLSv12() throws Throwable {
        testSessionTickets(SslProvider.OPENSSL, PROTOCOL_TLS_V1_2, true);
    }


    private static void testSessionTickets(SslProvider provider, String protocol, boolean withKey) throws Throwable {
        assumeTrue(OpenSsl.isAvailable());
        final SslContext sslClientCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .sslProvider(provider)
                .protocols(protocol)
                .build();

        // Explicit enable session cache as it's disabled by default atm.
        ((OpenSslContext) sslClientCtx).sessionContext()
                .setSessionCacheEnabled(true);

        final SelfSignedCertificate cert = new SelfSignedCertificate();
        final SslContext sslServerCtx = SslContextBuilder.forServer(cert.key(), cert.cert())
                .sslProvider(provider)
                .protocols(protocol)
                .build();


        if (withKey) {
            OpenSslSessionTicketKey key = new OpenSslSessionTicketKey(new byte[OpenSslSessionTicketKey.NAME_SIZE],
                    new byte[OpenSslSessionTicketKey.HMAC_KEY_SIZE], new byte[OpenSslSessionTicketKey.AES_KEY_SIZE]);
            ((OpenSslSessionContext) sslClientCtx.sessionContext()).setTicketKeys(key);
//            ((OpenSslSessionContext) sslServerCtx.sessionContext()).setTicketKeys(key);
            // use automic
//            SSLContext.clearOptions(((OpenSslSessionContext) sslServerCtx.sessionContext()).context.ctx, SSL_OP_NO_TICKET);
        } else {
            ((OpenSslSessionContext) sslClientCtx.sessionContext()).setTicketKeys();
            ((OpenSslSessionContext) sslServerCtx.sessionContext()).setTicketKeys();
        }

        EventLoopGroup group = new NioEventLoopGroup();
        Channel sc = null;
        final byte[] bytes = new byte[96];
        PlatformDependent.threadLocalRandom().nextBytes(bytes);
        try {
            final AtomicReference<AssertionError> assertErrorRef = new AtomicReference<AssertionError>();
//            sc = new ServerBootstrap()
//                    .group(group)
//                    .channel(NioServerSocketChannel.class)
//                    .childHandler(new ChannelInitializer<Channel>() {
//                        @Override
//                        protected void initChannel(Channel ch) {
//                            final SslHandler sslHandler = sslServerCtx.newHandler(ch.alloc());
//                            ch.pipeline().addLast(sslServerCtx.newHandler(UnpooledByteBufAllocator.DEFAULT));
//                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
//
//                                private int handshakeCount;
//
//                                @Override
//                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt)  {
//                                    if (evt instanceof SslHandshakeCompletionEvent) {
//                                        handshakeCount++;
//                                        ReferenceCountedOpenSslEngine engine =
//                                                (ReferenceCountedOpenSslEngine) sslHandler.engine();
//                                        // This test only works for non TLSv1.3 as TLSv1.3 will establish sessions after
//                                        // the handshake is done.
//                                        // See https://www.openssl.org/docs/man1.1.1/man3/SSL_CTX_sess_set_get_cb.html
//                                        if (!PROTOCOL_TLS_V1_3.equals(engine.getSession().getProtocol())) {
//                                            // First should not re-use the session
//                                            try {
////                                                assertEquals(handshakeCount > 1, engine.isSessionReused());
//                                            } catch (AssertionError error) {
//                                                assertErrorRef.set(error);
//                                                return;
//                                            }
//                                        }
//
//                                        ctx.writeAndFlush(Unpooled.wrappedBuffer(bytes));
//                                    }
//                                }
//                            });
//                        }
//                    })
//                    .bind(new InetSocketAddress("127.0.0.1", 8333)).syncUninterruptibly().channel();

//            InetSocketAddress serverAddr = (InetSocketAddress) sc.localAddress();

            InetSocketAddress serverAddr = new InetSocketAddress("10.177.46.61", 18899);
            testSessionTickets(serverAddr, group, sslClientCtx, bytes, false);
            testSessionTickets(serverAddr, group, sslClientCtx, bytes, true);
            testSessionTickets(serverAddr, group, sslClientCtx, bytes, true);
            testSessionTickets(serverAddr, group, sslClientCtx, bytes, true);
            AssertionError error = assertErrorRef.get();
            if (error != null) {
                throw error;
            }

        } finally {
//            if (sc != null) {
//                sc.close().syncUninterruptibly();
//            }
//            group.shutdownGracefully();
//            ReferenceCountUtil.release(sslClientCtx);
        }

        Thread.sleep(Long.MAX_VALUE);
    }

    private static void testSessionTickets(InetSocketAddress serverAddress, EventLoopGroup group,
                                           SslContext sslClientCtx, final byte[] bytes, boolean isReused)
            throws Throwable {
        Channel cc = null;
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
        try {
            final SslHandler clientSslHandler = sslClientCtx.newHandler(UnpooledByteBufAllocator.DEFAULT,
                    serverAddress.getAddress().getHostAddress(), serverAddress.getPort());

            ChannelFuture future = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(clientSslHandler);
                            ch.pipeline().addLast(new ByteToMessageDecoder() {

                                @Override
                                protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
                                    if (in.readableBytes() == bytes.length) {
                                        queue.add(in.readBytes(bytes.length));
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    queue.add(cause);
                                }
                            });
                        }
                    }).connect(serverAddress);
            cc = future.syncUninterruptibly().channel();

            assertTrue(clientSslHandler.handshakeFuture().sync().isSuccess());

            ReferenceCountedOpenSslEngine engine = (ReferenceCountedOpenSslEngine) clientSslHandler.engine();
            // This test only works for non TLSv1.3 as TLSv1.3 will establish sessions after
            // the handshake is done.
            // See https://www.openssl.org/docs/man1.1.1/man3/SSL_CTX_sess_set_get_cb.html
            if (!SslUtils.PROTOCOL_TLS_V1_3.equals(engine.getSession().getProtocol())) {
                System.out.println("===isReused:" + isReused + ", eq:" + Arrays.toString(engine.getSession().getId()) + "--" +engine.getSession());
//                assertEquals(isReused, engine.isSessionReused());
            }
            Object obj = queue.take();
            if (obj instanceof ByteBuf) {
                ByteBuf buffer = (ByteBuf) obj;
                ByteBuf expected = Unpooled.wrappedBuffer(bytes);
                try {
                    assertEquals(expected, buffer);
                } finally {
                    expected.release();
                    buffer.release();
                }
            } else {
                throw (Throwable) obj;
            }
        } finally {
            if (cc != null) {
                cc.close().syncUninterruptibly();
            }
        }
    }
}
