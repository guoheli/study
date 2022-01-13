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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.handler.codec.haproxy.*;
import io.netty.util.CharsetUtil;


import static io.netty.buffer.Unpooled.buffer;
import static io.netty.handler.ssl.HAProxyServer.PORT;

public final class HAProxyClient {

    private static final String HOST = System.getProperty("host", "127.0.0.1");

    public static void main(String[] args) throws Exception {


        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new HAProxyHandler());

            // Start the connection attempt.
            Channel ch = b.connect(HOST, 18898).sync().channel();

            final ByteBuf validHeaderV2 = buffer();
            validHeaderV2.writeByte(0x0D);
            validHeaderV2.writeByte(0x0A);
            validHeaderV2.writeByte(0x0D);
            validHeaderV2.writeByte(0x0A);
            validHeaderV2.writeByte(0x00);
            validHeaderV2.writeByte(0x0D);
            validHeaderV2.writeByte(0x0A);
            validHeaderV2.writeByte(0x51);
            validHeaderV2.writeByte(0x55);
            validHeaderV2.writeByte(0x49);
            validHeaderV2.writeByte(0x54);
            validHeaderV2.writeByte(0x0A);
            ProtocolDetectionResult<HAProxyProtocolVersion>  result = HAProxyMessageDecoder.detectProtocol(validHeaderV2);
            System.out.println(result.state());
            ch.writeAndFlush(validHeaderV2);

            ch.writeAndFlush(Unpooled.copiedBuffer("Hello World!", CharsetUtil.US_ASCII)).sync();
            ch.writeAndFlush(Unpooled.copiedBuffer("Bye now!", CharsetUtil.US_ASCII)).sync();
            ch.close().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
