/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.oped.apm.rpc.codec;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.rpc.client.WriteFailFutureListener;
import com.baidu.oped.apm.rpc.packet.ClientClosePacket;
import com.baidu.oped.apm.rpc.packet.ControlHandshakePacket;
import com.baidu.oped.apm.rpc.packet.ControlHandshakeResponsePacket;
import com.baidu.oped.apm.rpc.packet.PacketType;
import com.baidu.oped.apm.rpc.packet.PingPacket;
import com.baidu.oped.apm.rpc.packet.PongPacket;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.packet.ResponsePacket;
import com.baidu.oped.apm.rpc.packet.SendPacket;
import com.baidu.oped.apm.rpc.packet.ServerClosePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamClosePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreateFailPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreatePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreateSuccessPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamPingPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamPongPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamResponsePacket;

/**
 * @author emeroad
 * @author koo.taejin
 */
public class PacketDecoder extends FrameDecoder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WriteFailFutureListener pongWriteFutureListener = new WriteFailFutureListener(logger, "pong write fail.", "pong write success.");

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        if (buffer.readableBytes() < 2) {
            return null;
        }
        buffer.markReaderIndex();
        final short packetType = buffer.readShort();
        switch (packetType) {
            case PacketType.APPLICATION_SEND:
                return readSend(packetType, buffer);
            case PacketType.APPLICATION_REQUEST:
                return readRequest(packetType, buffer);
            case PacketType.APPLICATION_RESPONSE:
                return readResponse(packetType, buffer);
            case PacketType.APPLICATION_STREAM_CREATE:
                return readStreamCreate(packetType, buffer);
            case PacketType.APPLICATION_STREAM_CLOSE:
                return readStreamClose(packetType, buffer);
            case PacketType.APPLICATION_STREAM_CREATE_SUCCESS:
                return readStreamCreateSuccess(packetType, buffer);
            case PacketType.APPLICATION_STREAM_CREATE_FAIL:
                return readStreamCreateFail(packetType, buffer);
            case PacketType.APPLICATION_STREAM_RESPONSE:
                return readStreamData(packetType, buffer);
            case PacketType.APPLICATION_STREAM_PING:
                return readStreamPing(packetType, buffer);
            case PacketType.APPLICATION_STREAM_PONG:
                return readStreamPong(packetType, buffer);
            case PacketType.CONTROL_CLIENT_CLOSE:
                return readControlClientClose(packetType, buffer);
            case PacketType.CONTROL_SERVER_CLOSE:
                return readControlServerClose(packetType, buffer);
            case PacketType.CONTROL_PING:
                readPing(packetType, buffer);
                sendPong(channel);
                // just drop ping
                return null;
            case PacketType.CONTROL_PONG:
                logger.debug("receive pong. {}", channel);
                readPong(packetType, buffer);
                // just also drop pong.
                return null;
            case PacketType.CONTROL_HANDSHAKE:
                return readEnableWorker(packetType, buffer);
            case PacketType.CONTROL_HANDSHAKE_RESPONSE:
                return readEnableWorkerConfirm(packetType, buffer);
        }
        logger.error("invalid packetType received. packetType:{}, channel:{}", packetType, channel);
        channel.close();
        return null;
    }

    private void sendPong(Channel channel) {

        // a "pong" responds to a "ping" automatically.
        logger.debug("received ping. sending pong. {}", channel);
        ChannelFuture write = channel.write(PongPacket.PONG_PACKET);
        write.addListener(pongWriteFutureListener);
    }


    private Object readControlClientClose(short packetType, ChannelBuffer buffer) {
        return ClientClosePacket.readBuffer(packetType, buffer);
    }

    private Object readControlServerClose(short packetType, ChannelBuffer buffer) {
        return ServerClosePacket.readBuffer(packetType, buffer);
    }

    private Object readPong(short packetType, ChannelBuffer buffer) {
        return PongPacket.readBuffer(packetType, buffer);
    }

    private Object readPing(short packetType, ChannelBuffer buffer) {
        return PingPacket.readBuffer(packetType, buffer);
    }


    private Object readSend(short packetType, ChannelBuffer buffer) {
        return SendPacket.readBuffer(packetType, buffer);
    }


    private Object readRequest(short packetType, ChannelBuffer buffer) {
        return RequestPacket.readBuffer(packetType, buffer);
    }

    private Object readResponse(short packetType, ChannelBuffer buffer) {
        return ResponsePacket.readBuffer(packetType, buffer);
    }



    private Object readStreamCreate(short packetType, ChannelBuffer buffer) {
        return StreamCreatePacket.readBuffer(packetType, buffer);
    }


    private Object readStreamCreateSuccess(short packetType, ChannelBuffer buffer) {
        return StreamCreateSuccessPacket.readBuffer(packetType, buffer);
    }

    private Object readStreamCreateFail(short packetType, ChannelBuffer buffer) {
        return StreamCreateFailPacket.readBuffer(packetType, buffer);
    }

    private Object readStreamData(short packetType, ChannelBuffer buffer) {
        return StreamResponsePacket.readBuffer(packetType, buffer);
    }
    
    private Object readStreamPong(short packetType, ChannelBuffer buffer) {
        return StreamPongPacket.readBuffer(packetType, buffer);
    }

    private Object readStreamPing(short packetType, ChannelBuffer buffer) {
        return StreamPingPacket.readBuffer(packetType, buffer);
    }



    private Object readStreamClose(short packetType, ChannelBuffer buffer) {
        return StreamClosePacket.readBuffer(packetType, buffer);
    }

    private Object readEnableWorker(short packetType, ChannelBuffer buffer) {
        return ControlHandshakePacket.readBuffer(packetType, buffer);
    }

    private Object readEnableWorkerConfirm(short packetType, ChannelBuffer buffer) {
        return ControlHandshakeResponsePacket.readBuffer(packetType, buffer);
    }

}
