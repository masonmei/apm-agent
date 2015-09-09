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

package com.baidu.oped.apm.profiler.sender;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.profiler.sender.TcpDataSender;
import com.baidu.oped.apm.rpc.PinpointSocketException;
import com.baidu.oped.apm.rpc.client.PinpointSocket;
import com.baidu.oped.apm.rpc.client.PinpointSocketFactory;
import com.baidu.oped.apm.rpc.packet.HandshakeResponseCode;
import com.baidu.oped.apm.rpc.packet.HandshakeResponseType;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.packet.SendPacket;
import com.baidu.oped.apm.rpc.server.PinpointServerSocket;
import com.baidu.oped.apm.rpc.server.ServerMessageListener;
import com.baidu.oped.apm.rpc.server.SocketChannel;
import com.baidu.oped.apm.thrift.dto.TApiMetaData;

/**
 * @author emeroad
 */
public class TcpDataSenderTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int PORT = 10050;
    public static final String HOST = "127.0.0.1";

    private PinpointServerSocket server;
    private CountDownLatch sendLatch;

    @Before
    public void serverStart() {
        server = new PinpointServerSocket();
        server.setMessageListener(new ServerMessageListener() {

            @Override
            public void handleSend(SendPacket sendPacket, SocketChannel channel) {
                logger.info("handleSend:{}", sendPacket);
                if (sendLatch != null) {
                    sendLatch.countDown();
                }
            }

            @Override
            public void handleRequest(RequestPacket requestPacket, SocketChannel channel) {
                logger.info("handleRequest:{}", requestPacket);
            }
            
            @Override
            public HandshakeResponseCode handleHandshake(Map arg0) {
                return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;
            }
        });
        server.bind(HOST, PORT);
    }

    @After
    public void serverShutdown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    public void connectAndSend() throws InterruptedException {
        this.sendLatch = new CountDownLatch(2);

        PinpointSocketFactory socketFactory = createPinpointSocketFactory();
        
        PinpointSocket socket = createPinpointSocket(HOST, PORT, socketFactory);
        
        TcpDataSender sender = new TcpDataSender(socket);
        try {
            sender.send(new TApiMetaData("test", System.currentTimeMillis(), 1, "TestApi"));
            sender.send(new TApiMetaData("test", System.currentTimeMillis(), 1, "TestApi"));


            boolean received = sendLatch.await(1000, TimeUnit.MILLISECONDS);
            Assert.assertTrue(received);
        } finally {
            sender.stop();
            
            if (socket != null) {
                socket.close();
            }
            
            if (socketFactory != null) {
                socketFactory.release();
            }
        }
    }
    
    private PinpointSocketFactory createPinpointSocketFactory() {
        PinpointSocketFactory pinpointSocketFactory = new PinpointSocketFactory();
        pinpointSocketFactory.setTimeoutMillis(1000 * 5);
        pinpointSocketFactory.setProperties(Collections.EMPTY_MAP);

        return pinpointSocketFactory;
    }
    
    private PinpointSocket createPinpointSocket(String host, int port, PinpointSocketFactory factory) {
        PinpointSocket socket = null;
        for (int i = 0; i < 3; i++) {
            try {
                socket = factory.connect(host, port);
                logger.info("tcp connect success:{}/{}", host, port);
                return socket;
            } catch (PinpointSocketException e) {
                logger.warn("tcp connect fail:{}/{} try reconnect, retryCount:{}", host, port, i);
            }
        }
        logger.warn("change background tcp connect mode  {}/{} ", host, port);
        socket = factory.scheduledConnect(host, port);

        return socket;
    }
}
