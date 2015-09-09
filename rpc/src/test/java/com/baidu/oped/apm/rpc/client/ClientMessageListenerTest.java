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

package com.baidu.oped.apm.rpc.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.rpc.packet.HandshakeResponseCode;
import com.baidu.oped.apm.rpc.packet.HandshakeResponseType;
import com.baidu.oped.apm.rpc.server.ChannelContext;
import com.baidu.oped.apm.rpc.server.PinpointServerSocket;
import com.baidu.oped.apm.rpc.server.SimpleLoggingServerMessageListener;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils.EchoClientListener;

public class ClientMessageListenerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static int bindPort;
    
    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = PinpointRPCTestUtils.findAvailablePort();
    }
    
    @Test
    public void clientMessageListenerTest1() throws InterruptedException {
        PinpointServerSocket serverSocket = PinpointRPCTestUtils.createServerSocket(bindPort, new AlwaysHandshakeSuccessListener());

        EchoClientListener echoMessageListener = new EchoClientListener();
        PinpointSocketFactory clientSocketFactory = PinpointRPCTestUtils.createSocketFactory(PinpointRPCTestUtils.getParams(), echoMessageListener);

        try {
            PinpointSocket socket = clientSocketFactory.connect("127.0.0.1", bindPort);
            Thread.sleep(500);

            List<ChannelContext> channelContextList = serverSocket.getDuplexCommunicationChannelContext();
            if (channelContextList.size() != 1) {
                Assert.fail();
            }

            ChannelContext channelContext = channelContextList.get(0);
            assertSendMessage(channelContextList.get(0), "simple", echoMessageListener);
            assertRequestMessage(channelContext, "request", echoMessageListener);

            PinpointRPCTestUtils.close(socket);
        } finally {
            clientSocketFactory.release();
            PinpointRPCTestUtils.close(serverSocket);
        }
    }

    @Test
    public void clientMessageListenerTest2() throws InterruptedException {
        PinpointServerSocket serverSocket = PinpointRPCTestUtils.createServerSocket(bindPort, new AlwaysHandshakeSuccessListener());

        EchoClientListener echoMessageListener1 = PinpointRPCTestUtils.createEchoClientListener();
        PinpointSocketFactory clientSocketFactory1 = PinpointRPCTestUtils.createSocketFactory(PinpointRPCTestUtils.getParams(), echoMessageListener1);
        
        EchoClientListener echoMessageListener2 = PinpointRPCTestUtils.createEchoClientListener();
        PinpointSocketFactory clientSocketFactory2 = PinpointRPCTestUtils.createSocketFactory(PinpointRPCTestUtils.getParams(), echoMessageListener2);

        try {
            PinpointSocket socket = clientSocketFactory1.connect("127.0.0.1", bindPort);
            PinpointSocket socket2 = clientSocketFactory2.connect("127.0.0.1", bindPort);

            Thread.sleep(500);

            List<ChannelContext> channelContextList = serverSocket.getDuplexCommunicationChannelContext();
            if (channelContextList.size() != 2) {
                Assert.fail();
            }

            // channelcontext matching error
            ChannelContext channelContext = channelContextList.get(0);
            assertRequestMessage(channelContext, "socket1", null);

            ChannelContext channelContext2 = channelContextList.get(1);
            assertRequestMessage(channelContext2, "socket2", null);

            Assert.assertEquals(1, echoMessageListener1.getRequestPacketRepository().size());
            Assert.assertEquals(1, echoMessageListener2.getRequestPacketRepository().size());
            
            PinpointRPCTestUtils.close(socket, socket2);
        } finally {
            clientSocketFactory1.release();
            clientSocketFactory2.release();

            PinpointRPCTestUtils.close(serverSocket);
        }
    }

    private void assertSendMessage(ChannelContext channelContext, String message, EchoClientListener echoMessageListener) throws InterruptedException {
        channelContext.getSocketChannel().sendMessage(message.getBytes());
        Thread.sleep(100);

        Assert.assertEquals(message, new String(echoMessageListener.getSendPacketRepository().get(0).getPayload()));
    }

    private void assertRequestMessage(ChannelContext channelContext, String message, EchoClientListener echoMessageListener) throws InterruptedException {
        byte[] response = PinpointRPCTestUtils.request(channelContext, message.getBytes());
        Assert.assertEquals(message, new String(response));

        if (echoMessageListener != null) {
            Assert.assertEquals(message, new String(echoMessageListener.getRequestPacketRepository().get(0).getPayload()));
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
