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

package com.baidu.oped.apm.rpc.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.Timer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.rpc.client.PinpointClientSocketHandshaker;
import com.baidu.oped.apm.rpc.client.PinpointSocket;
import com.baidu.oped.apm.rpc.client.PinpointSocketFactory;
import com.baidu.oped.apm.rpc.packet.HandshakeResponseCode;
import com.baidu.oped.apm.rpc.packet.HandshakeResponseType;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils;
import com.baidu.oped.apm.rpc.util.TimerFactory;

public class HandshakeTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static Timer timer = null;

    private static int bindPort;
    
    @BeforeClass
    public static void setUp() throws IOException {
        timer = TimerFactory.createHashedWheelTimer(HandshakeTest.class.getSimpleName(), 100, TimeUnit.MILLISECONDS, 512);
        bindPort = PinpointRPCTestUtils.findAvailablePort();
    }

    @AfterClass
    public static void tearDown() {
        if (timer != null) {
            timer.stop();
        }
    }

    // simple test
    @Test
    public void handshakeTest1() throws InterruptedException {
        PinpointServerSocket serverSocket = PinpointRPCTestUtils.createServerSocket(bindPort, new AlwaysHandshakeSuccessListener());

        PinpointSocketFactory clientSocketFactory1 = PinpointRPCTestUtils.createSocketFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
        PinpointSocketFactory clientSocketFactory2 = PinpointRPCTestUtils.createSocketFactory(PinpointRPCTestUtils.getParams(), null);
        try {
            PinpointSocket socket = clientSocketFactory1.connect("127.0.0.1", bindPort);
            PinpointSocket socket2 = clientSocketFactory2.connect("127.0.0.1", bindPort);

            Thread.sleep(500);

            List<ChannelContext> channelContextList = serverSocket.getDuplexCommunicationChannelContext();
            if (channelContextList.size() != 2) {
                Assert.fail();
            }

            PinpointRPCTestUtils.close(socket, socket2);
        } finally {
            clientSocketFactory1.release();
            clientSocketFactory2.release();

            PinpointRPCTestUtils.close(serverSocket);
        }
    }

    @Test
    public void handshakeTest2() throws InterruptedException {
        PinpointServerSocket serverSocket = PinpointRPCTestUtils.createServerSocket(bindPort, new AlwaysHandshakeSuccessListener());

        Map params = PinpointRPCTestUtils.getParams();
        
        PinpointSocketFactory clientSocketFactory1 = PinpointRPCTestUtils.createSocketFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());

        try {
            PinpointSocket socket = clientSocketFactory1.connect("127.0.0.1", bindPort);
            Thread.sleep(500);

            ChannelContext channelContext = getChannelContext("application", "agent", (Long) params.get(AgentHandshakePropertyType.START_TIMESTAMP.getName()), serverSocket.getDuplexCommunicationChannelContext());
            Assert.assertNotNull(channelContext);

            channelContext = getChannelContext("application", "agent", (Long) params.get(AgentHandshakePropertyType.START_TIMESTAMP.getName()) + 1,
                    serverSocket.getDuplexCommunicationChannelContext());
            Assert.assertNull(channelContext);

            PinpointRPCTestUtils.close(socket);
        } finally {
            clientSocketFactory1.release();
            PinpointRPCTestUtils.close(serverSocket);
        }
    }

    @Test
    public void testExecuteCompleteWithoutStart() {
        int retryInterval = 100;
        int maxHandshakeCount = 10;

        PinpointClientSocketHandshaker handshaker = new PinpointClientSocketHandshaker(timer, retryInterval, maxHandshakeCount);
        handshaker.handshakeComplete(null);

        Assert.assertEquals(null, handshaker.getHandshakeResult());

        Assert.assertTrue(handshaker.isFinished());
    }

    @Test
    public void testExecuteAbortWithoutStart() {
        int retryInterval = 100;
        int maxHandshakeCount = 10;

        PinpointClientSocketHandshaker handshaker = new PinpointClientSocketHandshaker(timer, retryInterval, maxHandshakeCount);
        handshaker.handshakeAbort();

        Assert.assertTrue(handshaker.isFinished());
    }

    private ChannelContext getChannelContext(String applicationName, String agentId, long startTimeMillis, List<ChannelContext> duplexChannelContextList) {
        if (applicationName == null) {
            return null;
        }

        if (agentId == null) {
            return null;
        }

        if (startTimeMillis <= 0) {
            return null;
        }

        List<ChannelContext> channelContextList = new ArrayList<ChannelContext>();

        for (ChannelContext eachContext : duplexChannelContextList) {
            if (eachContext.getCurrentStateCode() == PinpointServerSocketStateCode.RUN_DUPLEX) {
                Map agentProperties = eachContext.getChannelProperties();

                if (!applicationName.equals(agentProperties.get(AgentHandshakePropertyType.APPLICATION_NAME.getName()))) {
                    continue;
                }

                if (!agentId.equals(agentProperties.get(AgentHandshakePropertyType.AGENT_ID.getName()))) {
                    continue;
                }

                if (startTimeMillis != (Long) agentProperties.get(AgentHandshakePropertyType.START_TIMESTAMP.getName())) {
                    continue;
                }

                channelContextList.add(eachContext);
            }
        }

        if (channelContextList.size() == 0) {
            return null;
        }

        if (channelContextList.size() == 1) {
            return channelContextList.get(0);
        } else {
            logger.warn("Ambiguous Channel Context {}, {}, {} (Valid Agent list={}).", applicationName, agentId, startTimeMillis, channelContextList);
            return null;
        }
    }

    private class AlwaysHandshakeSuccessListener extends SimpleLoggingServerMessageListener {
        @Override
        public HandshakeResponseCode handleHandshake(Map properties) {
            logger.info("handleEnableWorker {}", properties);
            return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;

        }
    }

}
