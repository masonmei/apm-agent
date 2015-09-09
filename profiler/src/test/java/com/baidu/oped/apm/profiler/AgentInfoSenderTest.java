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

package com.baidu.oped.apm.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TException;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.context.ServerMetaDataHolder;
import com.baidu.oped.apm.profiler.AgentInfoSender;
import com.baidu.oped.apm.profiler.AgentInformation;
import com.baidu.oped.apm.profiler.context.DefaultServerMetaDataHolder;
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
import com.baidu.oped.apm.thrift.dto.TResult;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializer;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializerFactory;

public class AgentInfoSenderTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int PORT = 10050;
    public static final String HOST = "127.0.0.1";

    @Test
    public void agentInfoShouldBeSent() throws InterruptedException {
        final AtomicInteger requestCount = new AtomicInteger();
        final AtomicInteger successCount = new AtomicInteger();
        final long agentInfoSendRetryIntervalMs = 1000L;

        ResponseServerMessageListener serverListener = new ResponseServerMessageListener(requestCount, successCount);

        PinpointServerSocket server = createServer(serverListener);

        PinpointSocketFactory socketFactory = createPinpointSocketFactory();
        PinpointSocket socket = createPinpointSocket(HOST, PORT, socketFactory);

        TcpDataSender sender = new TcpDataSender(socket);
        AgentInfoSender agentInfoSender = new AgentInfoSender(sender, agentInfoSendRetryIntervalMs, getAgentInfo(), getServerMetaDataHolder());

        try {
            agentInfoSender.start();
            Thread.sleep(10000L);
        } finally {
            closeAll(server, agentInfoSender, socket, socketFactory);
        }
        assertEquals(1, requestCount.get());
        assertEquals(1, successCount.get());
    }

    @Test
    public void agentInfoShouldRetryUntilSuccess() throws InterruptedException {
        final AtomicInteger requestCount = new AtomicInteger();
        final AtomicInteger successCount = new AtomicInteger();
        final long agentInfoSendRetryIntervalMs = 1000L;
        final int expectedTriesUntilSuccess = 5;

        ResponseServerMessageListener serverListener = new ResponseServerMessageListener(requestCount, successCount, expectedTriesUntilSuccess);

        PinpointServerSocket server = createServer(serverListener);

        PinpointSocketFactory socketFactory = createPinpointSocketFactory();
        PinpointSocket socket = createPinpointSocket(HOST, PORT, socketFactory);

        TcpDataSender dataSender = new TcpDataSender(socket);
        AgentInfoSender agentInfoSender = new AgentInfoSender(dataSender, agentInfoSendRetryIntervalMs, getAgentInfo(), getServerMetaDataHolder());

        try {
            agentInfoSender.start();
            Thread.sleep(agentInfoSendRetryIntervalMs * expectedTriesUntilSuccess);
        } finally {
            closeAll(server, agentInfoSender, socket, socketFactory);
        }
        assertEquals(expectedTriesUntilSuccess, requestCount.get());
        assertEquals(1, successCount.get());
    }

    @Test
    public void agentInfoShouldBeSentOnlyOnceEvenAfterReconnect() throws InterruptedException {
        final AtomicInteger requestCount = new AtomicInteger();
        final AtomicInteger successCount = new AtomicInteger();
        final long agentInfoSendRetryIntervalMs = 1000L;

        ResponseServerMessageListener serverListener = new ResponseServerMessageListener(requestCount, successCount);

        PinpointSocketFactory socketFactory = createPinpointSocketFactory();
        PinpointSocket socket = createPinpointSocket(HOST, PORT, socketFactory);

        TcpDataSender dataSender = new TcpDataSender(socket);
        AgentInfoSender agentInfoSender = new AgentInfoSender(dataSender, agentInfoSendRetryIntervalMs, getAgentInfo(), getServerMetaDataHolder());

        try {
            agentInfoSender.start();
            createAndDeleteServer(serverListener, 5000L);
            Thread.sleep(1000L);
            createAndDeleteServer(serverListener, 5000L);
            Thread.sleep(1000L);
            createAndDeleteServer(serverListener, 5000L);
        } finally {
            closeAll(null, agentInfoSender, socket, socketFactory);
        }
        assertEquals(1, requestCount.get());
        assertEquals(1, successCount.get());
    }

    @Test
    public void agentInfoShouldKeepRetrying() throws InterruptedException {
        final AtomicInteger requestCount = new AtomicInteger();
        final AtomicInteger successCount = new AtomicInteger();
        final long agentInfoSendRetryIntervalMs = 1000L;
        final long minimumAgentInfoSendRetryCount = 10;

        ResponseServerMessageListener serverListener = new ResponseServerMessageListener(requestCount, successCount, Integer.MAX_VALUE);

        PinpointServerSocket server = createServer(serverListener);

        PinpointSocketFactory socketFactory = createPinpointSocketFactory();
        PinpointSocket socket = createPinpointSocket(HOST, PORT, socketFactory);

        TcpDataSender dataSender = new TcpDataSender(socket);
        AgentInfoSender agentInfoSender = new AgentInfoSender(dataSender, agentInfoSendRetryIntervalMs, getAgentInfo(), getServerMetaDataHolder());

        try {
            agentInfoSender.start();
            Thread.sleep(agentInfoSendRetryIntervalMs * minimumAgentInfoSendRetryCount);
        } finally {
            closeAll(server, agentInfoSender, socket, socketFactory);
        }
        assertTrue(requestCount.get() >= minimumAgentInfoSendRetryCount);
        assertEquals(0, successCount.get());
    }

    public void reconnectionStressTest() throws InterruptedException {
        final AtomicInteger requestCount = new AtomicInteger();
        final AtomicInteger successCount = new AtomicInteger();
        final long stressTestTime = 60 * 1000L;
        final int randomMaxTime = 3000;
        final long agentInfoSendRetryIntervalMs = 1000L;
        final int expectedTriesUntilSuccess = (int)stressTestTime / (randomMaxTime * 2);

        ResponseServerMessageListener serverListener = new ResponseServerMessageListener(requestCount, successCount, expectedTriesUntilSuccess);

        PinpointSocketFactory socketFactory = createPinpointSocketFactory();
        PinpointSocket socket = createPinpointSocket(HOST, PORT, socketFactory);

        TcpDataSender dataSender = new TcpDataSender(socket);
        AgentInfoSender agentInfoSender = new AgentInfoSender(dataSender, agentInfoSendRetryIntervalMs, getAgentInfo(), getServerMetaDataHolder());

        long startTime = System.currentTimeMillis();

        try {
            agentInfoSender.start();

            Random random = new Random(System.currentTimeMillis());

            while (System.currentTimeMillis() < startTime + stressTestTime) {
                createAndDeleteServer(serverListener, Math.abs(random.nextInt(randomMaxTime)));
                Thread.sleep(Math.abs(random.nextInt(1000)));
            }

        } finally {
            closeAll(null, agentInfoSender, socket, socketFactory);
        }
        assertEquals(1, successCount.get());
        assertEquals(expectedTriesUntilSuccess, requestCount.get());
    }

    private PinpointServerSocket createServer(ServerMessageListener listener) {
        PinpointServerSocket server = new PinpointServerSocket();
        // server.setMessageListener(new
        // NoResponseServerMessageListener(requestCount));
        server.setMessageListener(listener);
        server.bind(HOST, PORT);

        return server;
    }

    private void createAndDeleteServer(ServerMessageListener listner, long waitTimeMillis) throws InterruptedException {
        PinpointServerSocket server = null;
        try {
            server = createServer(listner);
            Thread.sleep(waitTimeMillis);
        } finally {
            if (server != null) {
                server.close();
            }
        }
    }

    private void closeAll(PinpointServerSocket server, AgentInfoSender agentInfoSender, PinpointSocket socket, PinpointSocketFactory factory) {
        if (server != null) {
            server.close();
        }

        if (agentInfoSender != null) {
            agentInfoSender.stop();
        }

        if (socket != null) {
            socket.close();
        }

        if (factory != null) {
            factory.release();
        }
    }

    private AgentInformation getAgentInfo() {
        AgentInformation agentInfo = new AgentInformation("agentId", "appName", System.currentTimeMillis(), 1111, "hostname", "127.0.0.1", (short)2, "1");
        return agentInfo;
    }

    private ServerMetaDataHolder getServerMetaDataHolder() {
        ServerMetaDataHolder serverMetaDataHolder = new DefaultServerMetaDataHolder(Collections.<String>emptyList());
        return serverMetaDataHolder;
    }

    class ResponseServerMessageListener implements ServerMessageListener {
        private final AtomicInteger requestCount;
        private final AtomicInteger successCount;

        private final int successCondition;

        public ResponseServerMessageListener(AtomicInteger requestCount, AtomicInteger successCount) {
            this(requestCount, successCount, 1);
        }

        public ResponseServerMessageListener(AtomicInteger requestCount, AtomicInteger successCount, int successCondition) {
            this.requestCount = requestCount;
            this.successCount = successCount;
            this.successCondition = successCondition;
        }

        @Override
        public void handleSend(SendPacket sendPacket, SocketChannel channel) {
            logger.info("handleSend:{}", sendPacket);

        }

        @Override
        public void handleRequest(RequestPacket requestPacket, SocketChannel channel) {
            int requestCount = this.requestCount.incrementAndGet();

            if (requestCount < successCondition) {
                return;
            }

            logger.info("handleRequest~~~:{}", requestPacket);

            try {
                HeaderTBaseSerializer serializer = HeaderTBaseSerializerFactory.DEFAULT_FACTORY.createSerializer();

                TResult result = new TResult(true);
                byte[] resultBytes = serializer.serialize(result);

                this.successCount.incrementAndGet();

                channel.sendResponseMessage(requestPacket, resultBytes);
            } catch (TException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public HandshakeResponseCode handleHandshake(Map arg0) {
            return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;
        }
    }
    
    private PinpointSocketFactory createPinpointSocketFactory() {
        PinpointSocketFactory pinpointSocketFactory = new PinpointSocketFactory();
        pinpointSocketFactory.setTimeoutMillis(1000 * 5);
        pinpointSocketFactory.setProperties(Collections.<String, Object>emptyMap());

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
