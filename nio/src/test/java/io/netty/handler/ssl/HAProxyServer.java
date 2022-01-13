/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.handler.ssl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.List;

public final class HAProxyServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "18898"));

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HAProxyServerInitializer());
            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    static class HAProxyServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(
                    new LoggingHandler(LogLevel.DEBUG),
//                    new HAProxyMessageDecoder(),
                    new ByteToMessageDecoder() {
                        @Override
                        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) throws Exception {
                            if(in.readableBytes() < 12) {
                                return;
                            }
                            ChannelPipeline pipeline = ctx.pipeline();
                            ProtocolDetectionResult<HAProxyProtocolVersion> result = HAProxyMessageDecoder.detectProtocol(in);
                            System.out.println(result.state());
                        }
                    });
//                    new SimpleChannelInboundHandler() {
//                        @Override
//                        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
//                            if (msg instanceof HAProxyMessage) {
//                                System.out.println("proxy message: " + msg);
//                            } else if (msg instanceof ByteBuf) {
//                                System.out.println("bytebuf message: " + ByteBufUtil.prettyHexDump((ByteBuf) msg));
//                            }
//                        }
//                    });
        }
    }
}
